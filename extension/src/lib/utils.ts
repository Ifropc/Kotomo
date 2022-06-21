import { DayPeriod } from './interface';
import { io } from "../../../build/js/packages/kotomo"

export const kotomo = io.github.ifropc.kotomo

export function getDisplayDateTime() {

  const d = new Date();

  return {
    period: getPeriod(d),
  };
}


export function getPeriod(date: Date): DayPeriod {
  const hours = date.getHours();

  // 5:00 AM — 11:59 AM => morning
  if (hours >= 5 && hours < 12) {
    return 'morning';
  }

  // 12:00 PM — 4:59 PM => afternoon
  if (hours >= 12 && hours < 17) {
    return 'afternoon';
  }

  // 5:00 PM — 4:59 AM => night
  return 'night';
}

export function getPalette(period: DayPeriod) {
  return {
    morning: '#282e54',
    afternoon: '#000000',
    night: '#ffdd91',
  }[period];
}
