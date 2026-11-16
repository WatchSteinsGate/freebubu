from playwright.sync_api import sync_playwright
from pathlib import Path
import json
PROJECT=Path(__file__).resolve().parents[1]
OUTPUT=PROJECT/'build/test-output';OUTPUT.mkdir(parents=True,exist_ok=True)
saved={};errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':1360,'height':1000})
 page.on('pageerror',lambda e:errors.append(str(e)))
 def route(r):
  if r.request.url.endswith('/api/state'):
   if r.request.method=='POST': saved.update(json.loads(r.request.post_data))
   r.fulfill(json={'name':'LocalTester','state':saved})
  else:r.fulfill(content_type='text/html',body=(PROJECT/'editor/editor.html').read_text().replace('__TOKEN__','test-token'))
 page.route('http://editor.test/**',route)
 page.goto('http://editor.test/');page.wait_for_timeout(1500)
 page.locator('[data-part=nick] input').first.fill('Nebula Test')
 page.locator('#save-nick').click();page.wait_for_function("document.querySelector('#nick-status').textContent==='Saved'")
 assert 'Nebula Test' in saved['nickname']['frames'][0]['nick']
 page.locator('[data-tab=cape]').click();page.wait_for_timeout(300)
 page.locator('#save-cape').click();page.wait_for_function("document.querySelector('#cape-status').textContent==='Saved'")
 assert saved['cape']['frames'][0]['png'].startswith('iVBOR')
 page.screenshot(path=str(OUTPUT/'cape-editor.png'),full_page=True)
 page.locator('[data-tab=nick]').click();page.screenshot(path=str(OUTPUT/'nickname-editor.png'),full_page=True)
 page.reload();page.wait_for_timeout(1000);assert page.locator('[data-part=nick] input').first.input_value()=='Nebula Test'
 assert not errors,errors
 print('PASS: nickname save, cape PNG save, reload persistence, no JavaScript exceptions (mocked mod API).')
 browser.close()
