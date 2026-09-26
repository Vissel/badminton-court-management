import { render, screen } from '@testing-library/react';
import App from './App';

test('renders the login screen when unauthenticated', async () => {
  render(<App />);
  const loginTexts = await screen.findAllByText(/Đăng nhập/i);
  expect(loginTexts.length).toBeGreaterThan(0);
});
