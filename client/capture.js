import puppeteer from 'puppeteer-core';

const CHROME_PATH = '/usr/bin/google-chrome';
const BASE_URL = 'http://localhost:5173';

const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function capture() {
  const browser = await puppeteer.launch({
    executablePath: CHROME_PATH,
    headless: true,
    args: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage'],
  });

  const page = await browser.newPage();
  await page.setViewport({ width: 1440, height: 900 });

  // 1. 登录页
  console.log('1/7 截取登录页...');
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle2' });
  await sleep(500);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/01-login.png' });
  console.log('✅ 01-login.png');

  // 2. 先拿到 token
  const loginData = await page.evaluate(async () => {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123' }),
    });
    return res.json();
  });

  await page.evaluate((data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('username', 'admin');
    localStorage.setItem('realName', '张姐');
    localStorage.setItem('role', 'ADMIN');
  }, loginData.data);

  // 3. 工作台
  console.log('2/7 截取工作台...');
  await page.goto(`${BASE_URL}/dashboard`, { waitUntil: 'networkidle2' });
  await sleep(1500);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/02-dashboard.png' });
  console.log('✅ 02-dashboard.png');

  // 4. 车辆台账
  console.log('3/7 截取车辆台账...');
  await page.goto(`${BASE_URL}/vehicles`, { waitUntil: 'networkidle2' });
  await sleep(1500);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/03-vehicles.png' });
  console.log('✅ 03-vehicles.png');

  // 5. 车辆详情
  console.log('4/7 截取车辆详情...');
  await page.goto(`${BASE_URL}/vehicles/1`, { waitUntil: 'networkidle2' });
  await sleep(1000);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/04-vehicle-detail.png' });
  console.log('✅ 04-vehicle-detail.png');

  // 6. 提醒中心
  console.log('5/7 截取提醒中心...');
  await page.goto(`${BASE_URL}/reminders`, { waitUntil: 'networkidle2' });
  await sleep(1000);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/05-reminders.png' });
  console.log('✅ 05-reminders.png');

  // 7. 统计报表
  console.log('6/7 截取统计报表...');
  await page.goto(`${BASE_URL}/reports`, { waitUntil: 'networkidle2' });
  await sleep(1000);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/06-reports.png' });
  console.log('✅ 06-reports.png');

  // 8. 系统设置
  console.log('7/7 截取系统设置...');
  await page.goto(`${BASE_URL}/settings`, { waitUntil: 'networkidle2' });
  await sleep(1000);
  await page.screenshot({ path: '/root/vehicle-management/screenshots/07-settings.png' });
  console.log('✅ 07-settings.png');

  await browser.close();
  console.log('\n🎉 全部 7 个页面截图完成！');
}

capture().catch(e => { console.error(e); process.exit(1); });
