from pathlib import Path
from playwright.sync_api import sync_playwright
import json
root=Path(__file__).resolve().parents[1];out=root/'build/test-output';out.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
 b=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--enable-unsafe-swiftshader']);page=b.new_page(viewport={'width':1440,'height':1100});errors=[]
 page.on('requestfailed',lambda r:print('FAILED REQUEST',r.url,r.failure));page.on('pageerror',lambda e:errors.append(str(e)));page.on('console',lambda m:print(m.type,m.text[:300]) if m.type=='error' else None)
 def route(r):
  if r.request.url.endswith('/api/state'):r.fulfill(json={'name':'Tester','state':{}})
  else:r.fulfill(content_type='text/html',body=(root/'editor/editor.html').read_text().replace('__TOKEN__','test-token'))
 page.route('http://editor.test/**',route);page.goto('http://editor.test/');page.wait_for_timeout(4000);print('BODY:',page.locator('body').inner_text()[:2000]);print('ERRORS:',errors);page.screenshot(path=str(out/'mirror-nickname-probe.png'),full_page=True)
 if page.get_by_role('button',name='Cape',exact=True).count():page.get_by_role('button',name='Cape',exact=True).click();page.wait_for_timeout(4000);print('CAPE:',page.locator('body').inner_text()[:2000]);page.screenshot(path=str(out/'mirror-cape-probe.png'),full_page=True)
 b.close()
