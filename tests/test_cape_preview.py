from pathlib import Path
from playwright.sync_api import sync_playwright,expect
from PIL import Image,ImageChops
import json,re,hashlib
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/test-output';OUT.mkdir(parents=True,exist_ok=True);errors=[];external=[]
source=(ROOT/'patcher/src/CosmeticsPatch.java').read_text();connect=re.search(r"connect-src [^;]+;",source)[0]
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--enable-unsafe-swiftshader']);page=b.new_page(viewport={'width':1440,'height':1100});page.on('console',lambda m:errors.append(m.text) if m.type=='error' else None);page.on('pageerror',lambda e:errors.append(str(e)));page.on('request',lambda r:external.append(r.url) if r.url.startswith(('http:','https:')) and not r.url.startswith('http://editor.test/') else None)
 def route(r):
  if r.request.url.endswith('/api/state'):r.fulfill(json={'name':'Tester','state':{}})
  else:r.fulfill(content_type='text/html',body=(ROOT/'editor/editor.html').read_text().replace('__TOKEN__','test-token'),headers={'Content-Security-Policy':"default-src 'self'; script-src 'nonce-test-token'; worker-src blob:; style-src 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: blob:; "+connect+" object-src 'none'"})
 page.route('http://editor.test/**',route);page.goto('http://editor.test/?type=cape');expect(page.get_by_role('button',name='Custom image',exact=True)).to_be_visible(timeout=120000);page.wait_for_timeout(1200);canvas=page.locator('#cape-editor canvas').first;default=OUT/'cape-preview-default.png';canvas.screenshot(path=str(default));im=Image.open(default).convert('RGB');visible=sum(min(rgb)>12 and max(rgb)-min(rgb)<8 for rgb in im.getdata());assert visible>5000,f'Default cape is blank: {visible} object pixels'
 box=canvas.bounding_box();x=box['x']+box['width']/2;y=box['y']+box['height']/2;page.mouse.move(x,y);page.mouse.down();page.mouse.move(x+100,y+35,steps=10);page.mouse.up();page.wait_for_timeout(900);rotated=OUT/'cape-preview-rotated.png';canvas.screenshot(path=str(rotated));diff=ImageChops.difference(im,Image.open(rotated).convert('RGB'));assert sum(max(rgb)>10 for rgb in diff.getdata())>2000,'Drag did not rotate the visible cape'
 page.get_by_role('button',name='Custom image',exact=True).click()
 with page.expect_file_chooser() as fc:page.get_by_role('button',name='Choose image for outside',exact=False).click()
 fc.value.set_files(ROOT/'tests/fixtures/face.gif');expect(page.get_by_role('button',name='Save cape',exact=True)).to_be_enabled(timeout=120000);page.wait_for_timeout(1200);frames=[]
 for i in range(6):
  f=OUT/f'cape-preview-animation-{i}.png';canvas.screenshot(path=str(f));frames.append(Image.open(f).convert('RGB'));page.wait_for_timeout(90)
 changes=max(sum(max(rgb)>15 for rgb in ImageChops.difference(frames[0],f).getdata()) for f in frames[1:]);assert changes>1000,f'Animated cape preview is static or blank: {changes} changed pixels';assert not errors,errors;assert not external,external
 print(json.dumps({'result':'PASS','default_cape_pixels':visible,'gif_changed_pixels':changes,'drag_rotation':'passed','external_requests':external,'console_errors':errors},indent=2));b.close()
