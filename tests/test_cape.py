from pathlib import Path
from playwright.sync_api import sync_playwright,expect
import json,sys
from PIL import Image
PROJECT=Path(__file__).resolve().parents[1];R=PROJECT/'build/test-output';R.mkdir(parents=True,exist_ok=True);FIXTURES=PROJECT/'tests/fixtures';live='--live' in sys.argv;saved={};errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--enable-unsafe-swiftshader'])
 page=browser.new_page(viewport={'width':1440,'height':1100},accept_downloads=True);page.on('pageerror',lambda e:errors.append(str(e)))
 if not live:
  def route(r):
   if r.request.url.endswith('/api/state'):
    if r.request.method=='POST':saved.clear();saved.update(json.loads(r.request.post_data))
    r.fulfill(json={'name':'Tester','state':saved})
   else:r.fulfill(content_type='text/html',body=(PROJECT/'editor/editor.html').read_text().replace('__TOKEN__','test-token'))
  page.route('http://editor.test/**',route)
 page.goto(('http://127.0.0.1:6969' if live else 'http://editor.test')+'/?type=cape');page.wait_for_timeout(1000)
 expect(page.locator('#cape-editor')).to_be_visible();assert page.locator('.reference-intro,.reference-brand').count()==0
 for key in ['outside','inside','left','right','top','bottom']:
  page.locator('#face-'+key).click();page.locator('#solid-mode').click();page.get_by_label('Cape face hex color',exact=True).fill('#123456');page.get_by_label('Cape face hex color',exact=True).dispatch_event('change');expect(page.get_by_label('Cape face color',exact=True)).to_have_value('#123456')
 page.locator('#face-outside').click();page.locator('#image-mode').click()
 with page.expect_file_chooser() as fc:page.locator('#choose-face-image').click()
 fc.value.set_files(FIXTURES/'face.png');expect(page.locator('#face-source-name')).to_have_text('face.png')
 with page.expect_file_chooser() as fc:page.get_by_role('button',name='Replace',exact=True).click()
 fc.value.set_files(FIXTURES/'face.jpg');expect(page.locator('#face-source-name')).to_have_text('face.jpg')
 for ext in ['png','jpg','webp','gif']:
  page.locator('#face-file').set_input_files(FIXTURES/('face.'+ext));expect(page.locator('#face-source-name')).to_have_text('face.'+ext)
  page.get_by_label('Zoom',exact=True).fill('2');expect(page.locator('#zoom-value')).to_have_text('2.00×')
  c=page.locator('#face-preview');box=c.bounding_box();page.mouse.move(box['x']+box['width']/2,box['y']+box['height']/2);page.mouse.down();page.mouse.move(box['x']+box['width']/2+30,box['y']+box['height']/2+20,steps=3);page.mouse.up();page.mouse.wheel(0,-100);page.wait_for_timeout(100)
  page.get_by_role('button',name='Center crop',exact=True).click();expect(page.locator('#zoom-value')).to_have_text('1.00×')
  print('PASS upload and crop',ext,flush=True)
 expect(page.locator('#download-cape')).to_have_text('Download GIF')
 buttons=page.locator('.atlas-choices button')
 for i in reversed(range(buttons.count())):
  buttons.nth(i).click();expect(buttons.nth(i)).to_have_attribute('aria-pressed','true')
 print('PASS choose/replace file dialogs and every available atlas-size button',flush=True)
 page.locator('#save-cape').click();expect(page.locator('#cape-status')).to_have_text('Saved')
 page.reload();page.wait_for_timeout(1000);expect(page.locator('#face-source-name')).to_have_text('face.gif');expect(page.locator('#download-cape')).to_have_text('Download GIF')
 with page.expect_download() as d:page.locator('#download-cape').click()
 d.value.save_as(R/'test-export.gif');im=Image.open(R/'test-export.gif');assert im.n_frames==2;assert [im.seek(i) or im.info['duration'] for i in range(2)]==[100,200]
 print('PASS GIF export: 2 frames, exact 100/200 ms timing, and save/reload',flush=True)
 page.screenshot(path=str(R/('cape-custom-image'+('-live' if live else '')+'.png')),full_page=True)
 page.get_by_label('Remove face image',exact=True).click();expect(page.locator('#face-image-options')).to_be_hidden()
 
 with page.expect_file_chooser() as fc:page.locator('#import-cape').click()
 fc.value.set_files(FIXTURES/'atlas.gif');expect(page.locator('#global-status')).to_have_text('Cape imported.');expect(page.locator('#download-cape')).to_have_text('Download GIF')
 print('PASS import animated cape atlas',flush=True)
 for key in ['outside','inside','left','right','top','bottom']:page.locator('#face-'+key).click();page.get_by_label('Remove face image',exact=True).click()
 with page.expect_download() as d:page.locator('#download-cape').click()
 d.value.save_as(R/'test-export.png');assert Image.open(R/'test-export.png').size==(64,32)
 page.locator('#save-cape').click();expect(page.locator('#cape-status')).to_have_text('Saved');page.screenshot(path=str(R/('cape-solid-live.png' if live else 'cape-solid.png')),full_page=True)
 # Model drag changes its transform; native screenshot works without eval even under CSP.
 stage=page.locator('.cape-stage');box=stage.bounding_box();before=stage.screenshot();page.mouse.move(box['x']+100,box['y']+100);page.mouse.down();page.mouse.move(box['x']+180,box['y']+130);page.mouse.up();page.wait_for_timeout(500);assert stage.screenshot()!=before
 assert not errors,errors
 print('PASS solid mode, six face selectors/colors, remove, PNG export, 3D rotation; no JS errors',flush=True)
 browser.close()
