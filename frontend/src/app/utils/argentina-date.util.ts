const ARGENTINA_TIME_ZONE = 'America/Argentina/Buenos_Aires';

function resolveLocale(language?: string): string {
  const normalized = (language || '').trim().toLowerCase();
  if (normalized.startsWith('en')) {
    return 'en-US';
  }
  return 'es-AR';
}

export function formatArgentinaDate(value?: string, language?: string): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';

  return new Intl.DateTimeFormat(resolveLocale(language), {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    timeZone: ARGENTINA_TIME_ZONE
  }).format(date);
}

export function formatArgentinaDateTime(value?: string, language?: string): string {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';

  return new Intl.DateTimeFormat(resolveLocale(language), {
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: ARGENTINA_TIME_ZONE
  }).format(date);
}

export function argentinaDateCode(value?: string): string | undefined {
  if (!value) return undefined;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return undefined;

  const parts = new Intl.DateTimeFormat('en-CA', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: ARGENTINA_TIME_ZONE
  }).formatToParts(date);

  const day = parts.find(part => part.type === 'day')?.value;
  const month = parts.find(part => part.type === 'month')?.value;
  const year = parts.find(part => part.type === 'year')?.value;

  return day && month && year ? `${day}-${month}-${year}` : undefined;
}
