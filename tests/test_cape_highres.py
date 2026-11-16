from pathlib import Path
from playwright.sync_api import sync_playwright,expect
from PIL import Image,ImageDraw
import json,sys,re,base64,io
PROJECT=Path(__file__).resolve().parents[1];R=PROJECT/'build/test-output';R.mkdir(parents=True,exist_ok=True);FIXTURES=PROJECT/'tests/fixtures';fixture=FIXTURES/'15-frames.gif'
if not fixture.exists():
 frames=[]
 for i in range(15):
  im=Image.new('RGB',(640,1024),(20+i*10,90+i*5,110+i*3));ImageDraw.Draw(im).rectangle((100+i*10,100,400+i*10,900),fill=(220-i*10,80,150));frames.append(im)
 frames[0].save(fixture,save_all=True,append_images=frames[1:],duration=100,loop=0)
live='--live' in sys.argv;saved={};errors=[]
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--enable-unsafe-swiftshader']);page=b.new_page(viewport={'width':1440,'height':1100},accept_downloads=True);page.on('pageerror',lambda e:errors.append(str(e)))
 if not live:
  def route(r):
   if r.request.url.endswith('/api/state'):
    if r.request.method=='POST':saved.clear();saved.update(json.loads(r.request.post_data))
    r.fulfill(json={'name':'Tester','state':saved})
   else:r.fulfill(content_type='text/html',body=(PROJECT/'editor/editor.html').read_text().replace('__TOKEN__','test-token'))
  page.route('http://editor.test/**',route)
 page.goto(('http://127.0.0.1:6969' if live else 'http://editor.test')+'/?type=cape');page.wait_for_timeout(1000)
 # Remove prior test cosmetics for a deterministic first-source resolution choice.
 page.locator('#reset-state').click();expect(page.locator('#global-status')).to_have_text('Cosmetics reset.')
 page.locator('#image-mode').click();page.locator('#face-file').set_input_files(fixture)
 expect(page.locator('#face-source-name')).to_have_text('15-frames.gif',timeout=120000)
 for label in ['Full 4096×2048','Half 2048×1024','Quarter 1024×512','Eighth 512×256']:expect(page.get_by_role('button',name=label,exact=True)).to_be_visible()
 expect(page.get_by_role('button',name='Quarter 1024×512',exact=True)).to_have_attribute('aria-pressed','true')
 expect(page.get_by_label('Cape rendering progress')).to_be_visible()
 expect(page.locator('#download-cape')).to_be_enabled(timeout=180000)
 page.screenshot(path=str(R/('cape-15-frames-live.png' if live else 'cape-15-frames.png')),full_page=True)
 page.locator('#save-cape').click();expect(page.locator('#cape-status')).to_have_text('Saved',timeout=120000)
 if live:saved=page.request.get('http://127.0.0.1:6969/api/state',headers={'X-Cosmetics-Token':page.locator('script[nonce]').get_attribute('nonce')}).json()['state']
 assert len(saved['cape']['frames'])==15
 assert Image.open(io.BytesIO(base64.b64decode(saved['cape']['frames'][0]['png']))).size==(1024,512)
 with page.expect_download(timeout=120000) as d:page.locator('#download-cape').click()
 dest=R/'test-15-frames-export.gif';d.value.save_as(dest);im=Image.open(dest);assert im.n_frames==15 and im.size==(1024,512)
 for i in range(15):im.seek(i);assert im.info['duration']==100
 page.reload();expect(page.locator('#face-source-name')).to_have_text('15-frames.gif',timeout=120000);expect(page.locator('#download-cape')).to_be_enabled(timeout=180000)
 assert not errors,errors
 print('PASS: 15-frame GIF, visible render progress, Full/Half/Quarter/Eighth 4096/2048/1024/512 choices, 1024×512 PNG persistence, GIF export frame count/timing, source reload',flush=True)
 b.close()
