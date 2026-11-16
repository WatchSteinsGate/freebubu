from playwright.sync_api import sync_playwright, expect
from pathlib import Path
import json
PROJECT=Path(__file__).resolve().parents[1]
OUTPUT=PROJECT/'build/test-output';OUTPUT.mkdir(parents=True,exist_ok=True)
saved={};errors=[]
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':1360,'height':1000})
 page.on('pageerror',lambda e:errors.append(str(e)))
 page.goto('http://127.0.0.1:6969/');page.wait_for_timeout(1500)
 page.locator('[data-part=nick] input').first.fill('Nebula Test')
 page.locator('#save-nick').click();expect(page.locator('#nick-status')).to_have_text('Saved')

 page.locator('[data-tab=cape]').click();page.wait_for_timeout(300)
 page.locator('#save-cape').click();expect(page.locator('#cape-status')).to_have_text('Saved')

 page.screenshot(path=str(OUTPUT/'cape-editor-live.png'),full_page=True)
 page.locator('[data-tab=nick]').click();page.screenshot(path=str(OUTPUT/'nickname-editor-live.png'),full_page=True)
 page.reload();page.wait_for_timeout(1000);assert page.locator('[data-part=nick] input').first.input_value()=='Nebula Test'
 assert not errors,errors
 print('PASS: nickname save, cape PNG save, reload persistence, no JavaScript exceptions (actual running mod API).')
 browser.close()
