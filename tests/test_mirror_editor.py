from pathlib import Path
from playwright.sync_api import sync_playwright,expect
import json,sys,re,base64,io
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'build/test-output';OUT.mkdir(parents=True,exist_ok=True);FIX=ROOT/'tests/fixtures';live='--live' in sys.argv
saved={};errors=[];external=[]
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--enable-unsafe-swiftshader']);page=b.new_page(viewport={'width':1440,'height':1100},accept_downloads=True)
 page.on('pageerror',lambda e:errors.append(str(e)))
 def request(r):
  if r.url.startswith('http') and not r.url.startswith(('http://editor.test','http://127.0.0.1:6969')):external.append(r.url.split('?')[0])
 page.on('request',request)
 if not live:
  def route(r):
   if r.request.url.endswith('/api/state'):
    if r.request.method=='POST':saved.clear();saved.update(json.loads(r.request.post_data))
    r.fulfill(json={'name':'Tester','state':saved})
   else:r.fulfill(content_type='text/html',body=(ROOT/'editor/editor.html').read_text().replace('__TOKEN__','test-token'),headers={'Content-Security-Policy':"default-src 'self'; script-src 'nonce-test-token'; worker-src blob:; style-src 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: blob:; connect-src 'self' data: blob:; object-src 'none'"})
  page.route('http://editor.test/**',route)
 base='http://127.0.0.1:6969' if live else 'http://editor.test'
 page.goto(base+'/');expect(page.get_by_role('button',name='Save nickname',exact=True)).to_be_visible(timeout=120000)
 if live:
  with page.expect_navigation():page.get_by_role('button',name='Reset cosmetics',exact=True).click()
  expect(page.get_by_role('button',name='Save nickname',exact=True)).to_be_visible(timeout=120000)
 body=page.locator('body').inner_text();assert not any(x in body for x in ['Try it before','Login','Back to cosmetics','Drafts stay','Local'])
 boxes=page.locator('#nick-editor').get_by_role('textbox');expect(boxes).to_have_count(2);boxes.nth(1).fill('Mirror Nick')
 page.get_by_role('button',name='Save nickname',exact=True).click();expect(page.locator('#host-status')).to_have_text('Saved.',timeout=30000)
 page.reload();expect(page.locator('#nick-editor').get_by_role('textbox').nth(1)).to_have_value('Mirror Nick',timeout=120000)
 page.get_by_role('switch').first.click();expect(page.get_by_role('button',name='Add frame',exact=True)).to_be_visible();page.get_by_role('button',name='Add frame',exact=True).click();expect(page.get_by_role('button',name=re.compile('Frame 3'))).to_be_visible();page.get_by_role('button',name='Duplicate',exact=True).click();expect(page.get_by_role('button',name=re.compile('Frame 4'))).to_be_visible();page.get_by_role('button',name='Remove',exact=True).click();expect(page.get_by_role('button',name=re.compile('Frame 4'))).to_have_count(0)
 page.get_by_role('button',name='More styles',exact=True).last.click();page.get_by_role('button',name='Gradient',exact=True).click();page.get_by_role('button',name='Chroma',exact=True).click();page.keyboard.press('Escape');page.get_by_role('button',name='Save nickname',exact=True).click();expect(page.locator('#host-status')).to_have_text('Saved.');page.screenshot(path=str(OUT/'mirror-nickname.png'),full_page=True)
 print('PASS: actual mirror nickname component; editing, animation, add/duplicate/remove frames, save/reload',flush=True)
 page.locator('.editor-tabs').get_by_text('Cape',exact=True).click();expect(page.get_by_role('button',name='Import cape file',exact=True)).to_be_visible(timeout=60000)
 page.get_by_role('button',name='Outside',exact=True).click();page.get_by_role('button',name='Custom image',exact=True).click()
 with page.expect_file_chooser() as fc:page.get_by_role('button',name='Choose image for outside',exact=False).click()
 fc.value.set_files(FIX/'face.gif');expect(page.get_by_text('face.gif',exact=True)).to_be_visible(timeout=120000)
 zoom=page.get_by_role('slider',name='Thumb');print('Sliders:',page.get_by_role('slider').count(),flush=True)
 page.get_by_role('button',name='Center crop',exact=True).click()
 expect(page.get_by_role('button',name='Save cape',exact=True)).to_be_enabled(timeout=120000)
 with page.expect_download() as d:page.get_by_role('button',name='Download GIF',exact=True).click()
 d.value.save_as(OUT/'mirror-export.gif');gif=Image.open(OUT/'mirror-export.gif');assert gif.n_frames==2;assert [gif.seek(i) or gif.info['duration'] for i in range(2)]==[100,200]
 page.get_by_role('button',name='Save cape',exact=True).click();expect(page.locator('#host-status')).to_have_text('Saved.',timeout=120000)
 page.screenshot(path=str(OUT/'mirror-cape.png'),full_page=True)
 page.reload();expect(page.get_by_role('button',name='Save cape',exact=True)).to_be_enabled(timeout=120000);expect(page.get_by_text('Saved',exact=True)).to_be_visible()
 assert not errors,errors;assert not external,external
 print('PASS: actual mirror cape component, worker GIF decode/encode, 2-frame export timing, save/reload; no external requests or JS errors',flush=True)
 b.close()
