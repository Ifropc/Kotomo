import {getDisplayDateTime, kotomo, getPalette} from './utils';

export function renderDateTime() {
  console.log("hello!")
  console.log(kotomo.Greeter.greet())

  const { period } = getDisplayDateTime();
  const palette = getPalette(period);
  const root = document.getElementById('root');
  const headingDate = document.getElementById('date');
  const headingTime = document.getElementById('time');
  headingDate.innerText = kotomo.Greeter.greet();
    headingDate.style.color = palette;
  headingTime.style.color = palette;
  root.style.backgroundImage = `url('images/${period}.png')`;
}
