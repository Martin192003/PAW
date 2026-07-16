export function isApiUrl(url: string, apiBaseUrl: string): boolean {
  if (!url) return false;

  const basePath = apiPath(apiBaseUrl);
  const apiPrefixes = [apiBaseUrl, basePath, '/api'].filter(Boolean);

  if (!isAbsoluteUrl(url)) {
    return apiPrefixes.some(prefix => url.startsWith(prefix));
  }

  try {
    const parsed = new URL(url);
    const parsedBase = isAbsoluteUrl(apiBaseUrl) ? new URL(apiBaseUrl) : null;
    if (parsedBase && parsed.origin !== parsedBase.origin) {
      return false;
    }
    if (!parsedBase && !isAllowedApiOrigin(parsed)) {
      return false;
    }
    return apiPrefixes.some(prefix => parsed.pathname.startsWith(apiPath(prefix))) || hasApiPathSegment(parsed.pathname);
  } catch {
    return false;
  }
}

export function resolveApiUrl(pathOrUrl: string, apiBaseUrl: string): string {
  if (!pathOrUrl) return pathOrUrl;
  if (isAbsoluteUrl(pathOrUrl)) {
    return pathOrUrl;
  }

  const basePath = apiPath(apiBaseUrl);
  if (pathOrUrl.startsWith(apiBaseUrl) || pathOrUrl.startsWith(basePath) || pathOrUrl.startsWith('/api')) {
    return pathOrUrl;
  }

  const base = apiBaseUrl.endsWith('/') ? apiBaseUrl.slice(0, -1) : apiBaseUrl;
  const suffix = pathOrUrl.startsWith('/') ? pathOrUrl : `/${pathOrUrl}`;
  return `${base}${suffix}`;
}

function isAbsoluteUrl(url: string): boolean {
  return url.startsWith('http://') || url.startsWith('https://');
}

function hasApiPathSegment(pathname: string): boolean {
  return pathname === '/api' || pathname.includes('/api/');
}

function isAllowedApiOrigin(parsed: URL): boolean {
  if (typeof window === 'undefined') {
    return false;
  }

  if (parsed.origin === window.location.origin) {
    return true;
  }

  const currentHost = normalizeLocalHost(window.location.hostname);
  const parsedHost = normalizeLocalHost(parsed.hostname);
  return currentHost !== null && parsedHost !== null && currentHost === parsedHost;
}

function normalizeLocalHost(hostname: string): string | null {
  const normalized = hostname.replace(/^\[|\]$/g, '').toLowerCase();
  if (normalized === 'localhost' || normalized === '127.0.0.1' || normalized === '::1') {
    return 'localhost';
  }
  return null;
}

function apiPath(apiBaseUrl: string): string {
  if (!isAbsoluteUrl(apiBaseUrl)) {
    return apiBaseUrl;
  }

  try {
    return new URL(apiBaseUrl).pathname;
  } catch {
    return apiBaseUrl;
  }
}

export type LinkDto = {
  rel: string;
  href: string;
};

export type LinkMapInput =
  | Record<string, string | undefined>
  | LinkDto[]
  | { entry?: LinkDto[] }
  | null
  | undefined;

export function normalizeLinks(links: LinkMapInput): Record<string, string> | undefined {
  if (!links) {
    return undefined;
  }

  if (Array.isArray(links)) {
    return links.reduce<Record<string, string>>((acc, link) => {
      if (link && typeof link.rel === 'string' && typeof link.href === 'string') {
        acc[link.rel] = link.href;
      }
      return acc;
    }, {});
  }

  if ('entry' in links && Array.isArray(links.entry)) {
    return normalizeLinks(links.entry);
  }

  if (typeof links === 'object') {
    return Object.entries(links).reduce<Record<string, string>>((acc, [key, value]) => {
      if (typeof value === 'string' && value) {
        acc[key] = value;
      }
      return acc;
    }, {});
  }

  return undefined;
}

export function requireLink(
  links: LinkMapInput,
  key: string,
  context: string
): string {
  const normalized = normalizeLinks(links);
  const value = normalized?.[key];
  if (!value) {
    throw new Error(`Missing HATEOAS link '${key}' for ${context}.`);
  }
  return value;
}

export function requireAnyLink(
  links: LinkMapInput,
  keys: string[],
  context: string
): string {
  const normalized = normalizeLinks(links);
  for (const key of keys) {
    const value = normalized?.[key];
    if (value) {
      return value;
    }
  }
  throw new Error(`Missing HATEOAS link (${keys.join(', ')}) for ${context}.`);
}

export function itemLinksFromCollection(body: unknown): string[] | null {
  if (!Array.isArray(body)) {
    return null;
  }

  const links = body
    .filter((item): item is LinkDto => {
      return !!item
        && typeof item === 'object'
        && 'rel' in item
        && 'href' in item
        && typeof (item as LinkDto).rel === 'string'
        && typeof (item as LinkDto).href === 'string';
    })
    .filter(link => link.rel === 'item' || link.rel === 'self')
    .map(link => link.href);

  return links.length === body.length ? links : null;
}

export function totalFromHeader(raw: string | null, fallback: number): number {
  const parsed = raw === null ? NaN : Number(raw);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : fallback;
}
