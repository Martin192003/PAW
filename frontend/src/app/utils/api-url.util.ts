export function isApiUrl(url: string, apiBaseUrl: string): boolean {
  if (!url) return false;

  if (url.startsWith('http://') || url.startsWith('https://')) {
    try {
      const parsed = new URL(url);
      return parsed.pathname.startsWith(apiBaseUrl);
    } catch {
      return false;
    }
  }

  return url.startsWith(apiBaseUrl);
}

export function resolveApiUrl(pathOrUrl: string, apiBaseUrl: string): string {
  if (!pathOrUrl) return pathOrUrl;
  if (pathOrUrl.startsWith('http://') || pathOrUrl.startsWith('https://')) {
    return pathOrUrl;
  }

  if (pathOrUrl.startsWith(apiBaseUrl)) {
    return pathOrUrl;
  }

  const base = apiBaseUrl.endsWith('/') ? apiBaseUrl.slice(0, -1) : apiBaseUrl;
  const suffix = pathOrUrl.startsWith('/') ? pathOrUrl : `/${pathOrUrl}`;
  return `${base}${suffix}`;
}

export type LinkDto = {
  rel: string;
  href: string;
};

export type LinkMapInput = Record<string, string> | LinkDto[] | null | undefined;

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

  if (typeof links === 'object') {
    return links as Record<string, string>;
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
