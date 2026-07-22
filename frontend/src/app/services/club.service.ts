import { HttpClient, HttpContext, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { ClubLimits } from '../models/club-limits.model';
import { ClubPhoto } from '../models/club-photo.model';
import { ClubTable } from '../models/club-table.model';
import { Club, ClubPayload } from '../models/club.model';
import { Event } from '../models/event.model';
import { Layout } from '../models/layout.model';
import { PagedResponse } from '../models/paged-response.model';
import { Product } from '../models/product.model';
import { Review } from '../models/review.model';
import { itemLinksFromCollection, LinkMapInput, normalizeLinks, requireAnyLink, resolveApiUrl, totalFromHeader } from '../utils/api-url.util';
import { SKIP_API_ERROR_REDIRECT } from './api-error.interceptor';

export interface ClubUpdatePayload {
  clubName: string;
  location: string;
  priceRange: string;
  description: string;
  musicGenre: string;
  zone: string;
  isBar: boolean;
  cbu: string;
}

export interface ProductPayload {
  productName: string;
  description: string;
  basePrice: number;
  productType: string;
  isAvailable: boolean;
}

export interface TablePayload {
  tableNumber: number;
  capacity: number;
  price: number;
  discount: number;
  tableState: string;
}

export interface EventPayload {
  eventName: string;
  description: string;
  date: string;
}

@Injectable({
  providedIn: 'root'
})
export class ClubService {
  private readonly apiUrl = environment.apiBaseUrl;

  private readonly emptyFilter = {
    name: '',
    zone: '',
    genre: '',
    price: '',
    isBar: '',
    sort: ''
  };

  private readonly linkRels = {
    photos: ['photos', 'club-photos', 'gallery'],
    reviewPhotos: ['review-photos', 'reviewPhotos'],
    reviews: ['reviews', 'club-reviews'],
    events: ['events', 'club-events'],
    menu: ['menu', 'club-menu'],
    tables: ['tables', 'club-tables'],
    bookings: ['bookings', 'club-bookings'],
    layout: ['layout', 'club-layout'],
    cover: ['cover', 'club-cover'],
  };

  constructor(private readonly http: HttpClient) {}

  private normalizeClub(club: Club): Club {
    return {
      ...club,
      links: normalizeLinks(club.links)
    };
  }

  private normalizeClubs(clubs: Club[]): Club[] {
    return clubs.map(club => this.normalizeClub(club));
  }

  private normalizeProduct(product: Product): Product {
    return {
      ...product,
      links: normalizeLinks(product.links)
    };
  }

  private normalizeTable(table: ClubTable): ClubTable {
    return {
      ...table,
      links: normalizeLinks(table.links)
    };
  }

  private normalizeEvent(event: Event & { id?: string; name?: string }): Event {
    return {
      ...event,
      eventId: event.eventId || event.id || '',
      eventName: event.eventName || event.name || '',
      description: event.description || '',
      date: event.date || '',
      links: normalizeLinks(event.links)
    };
  }

  private resolveClubLink(
    links: unknown,
    rels: string[],
    context: string,
    fallback: string
  ): string {
    if (links == null) {
      return resolveApiUrl(fallback, this.apiUrl);
    }

    return resolveApiUrl(requireAnyLink(links as any, rels, context), this.apiUrl);
  }

  private missingLinkError(err: unknown): Observable<never> {
    return throwError(() => err instanceof Error ? err : new Error('Missing HATEOAS link.'));
  }

  private optionalApiContext(): HttpContext {
    return new HttpContext().set(SKIP_API_ERROR_REDIRECT, true);
  }

  getClubs(
    page: number,
    size: number,
    filters: Partial<{ name: string; zone: string; genre: string; price: string; isBar: string; sort: string }> = this.emptyFilter
  ): Observable<PagedResponse<Club>> {
    const normalized = { ...this.emptyFilter, ...filters };

    let params = new HttpParams()
      .set('page', page - 1)
      .set('size', size);

    if (normalized.name) params = params.set('name', normalized.name);
    if (normalized.zone) params = params.set('zone', normalized.zone);
    if (normalized.genre) params = params.set('genre', normalized.genre);
    if (normalized.price) params = params.set('price', normalized.price);
    if (normalized.isBar) params = params.set('isBar', normalized.isBar);
    if (normalized.sort) params = params.set('sort', normalized.sort);

    return this.http.get<Club[] | PagedResponse<Club>>(`${this.apiUrl}/clubs`, {
      params,
      observe: 'response'
    }).pipe(
      switchMap(response => this.clubPageFromResponse(response, page, size))
    );
  }

  getClubsByUser(userId: string, page: number, size: number): Observable<PagedResponse<Club>> {
    // La UI trabaja con paginas 1-based; la API es 0-based.
    const params = new HttpParams()
      .set('ownerId', userId)
      .set('page', page - 1)
      .set('size', size);

    return this.http.get<Club[] | PagedResponse<Club>>(
      `${this.apiUrl}/clubs`,
      { params, observe: 'response' }
    ).pipe(
      switchMap(response => this.clubPageFromResponse(response, page, size))
    );
  }

  createClub(payload: ClubPayload): Observable<Club> {
    return this.http.post<Club>(`${this.apiUrl}/clubs`, payload).pipe(
      map(club => this.normalizeClub(club))
    );
  }


  getClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  getManagedClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`, {
      params: new HttpParams().set('managed', 'true')
    }).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  getClubByLink(link: string): Observable<Club> {
    return this.http.get<Club>(resolveApiUrl(link, this.apiUrl)).pipe(
      map(club => this.normalizeClub(club))
    );
  }


  getCoverUrl(id: string): string {
    return resolveApiUrl(`${this.apiUrl}/clubs/${id}/cover`, this.apiUrl);
  }


  getPhotoUrl(photoId: string): string {
    return resolveApiUrl(`${this.apiUrl}/photos/${photoId}`, this.apiUrl);
  }

  getClubPhotos(id: string, links?: unknown): Observable<ClubPhoto[]> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.photos, `club ${id} photos`, `${this.apiUrl}/clubs/${id}/photos`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http
      .get<ClubPhoto[] | { photos?: ClubPhoto[] }>(url, {
        context: this.optionalApiContext(),
        observe: 'response'
      })
      .pipe(
        switchMap(response => this.photosFromResponse(response, id))
      );
  }

  getReviewPhotosByClub(id: string, links?: unknown): Observable<ClubPhoto[]> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviewPhotos, `club ${id} review photos`, `${this.apiUrl}/clubs/${id}/review-photos`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http
      .get<ClubPhoto[] | { photos?: ClubPhoto[] }>(url, {
        context: this.optionalApiContext(),
        observe: 'response'
      })
      .pipe(
        switchMap(response => this.photosFromResponse(response, id))
      );
  }

  getLayout(id: string, links?: unknown): Observable<Layout> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.layout, `club ${id} layout`, `${this.apiUrl}/clubs/${id}/layout`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Layout>(url, {
      context: this.optionalApiContext()
    });
  }

  uploadCover(clubId: string, file: File, links?: unknown): Observable<unknown> {
    const formData = new FormData();
    formData.append('file', file);
    const url = this.resolveClubLink(links, this.linkRels.cover, `club ${clubId} cover`, `${this.apiUrl}/clubs/${clubId}/cover`);
    return this.http.put(url, formData);
  }

  uploadLayout(clubId: string, file: File, links?: unknown): Observable<unknown> {
    const formData = new FormData();
    formData.append('file', file);

    const url = this.resolveClubLink(links, this.linkRels.layout, `club ${clubId} layout`, `${this.apiUrl}/clubs/${clubId}/layout`);
    return this.http.put(url, formData);
  }

  uploadGalleryPhotos(clubId: string, files: File[], links?: unknown): Observable<unknown> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.photos, `club ${clubId} photos`, `${this.apiUrl}/clubs/${clubId}/photos`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const uploads = files.map((file) => {
      const formData = new FormData();
      formData.append('file', file);
      return this.http.post(url, formData);
    });

    return forkJoin(uploads);
  }


  deleteGalleryPhoto(clubId: string, photoId: string, photoLinks?: LinkMapInput, clubLinks?: unknown): Observable<void> {
    const normalizedPhotoLinks = normalizeLinks(photoLinks);
    const deleteLink = normalizedPhotoLinks?.['delete'];
    if (deleteLink) {
      return this.http.delete<void>(resolveApiUrl(deleteLink, this.apiUrl));
    }

    return this.http.delete<void>(`${this.apiUrl}/photos/${encodeURIComponent(photoId)}`);
  }

  toAbsoluteUrl(path: string, clubId?: string): string {
    if (/^https?:\/\//i.test(path)) {
      return path;
    }
    if (!path && clubId) {
      return resolveApiUrl(`${this.apiUrl}/clubs/${clubId}/layout/image`, this.apiUrl);
    }
    const normalizedPath = path.startsWith('/') ? path.slice(1) : path;
    return resolveApiUrl(`/${normalizedPath}`, this.apiUrl);
  }

  getLimits(id: string): Observable<ClubLimits> {
    return this.http.get<ClubLimits>(`${this.apiUrl}/clubs/${id}/limits`);
  }

  getMenu(id: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.menu, `club ${id} menu`, `${this.apiUrl}/clubs/${id}/menu`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Product[]>(url, { observe: 'response' }).pipe(
      switchMap(response => this.collectionFromResponse(response, product => this.normalizeProduct(product)))
    );
  }

  getTables(id: string, date?: string, links?: unknown) {
    let params = new HttpParams();
    if (date) {
      params = params.set('date', date);
    }

    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.tables, `club ${id} tables`, `${this.apiUrl}/clubs/${id}/tables`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<ClubTable[]>(url, { params, observe: 'response' }).pipe(
      switchMap(response => this.collectionFromResponse(response, table => this.normalizeTable(table)))
    );
  }

  reserveTable(clubId: string, tableId: string, date: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(
        links,
        ['create-booking', ...this.linkRels.bookings],
        `club ${clubId} bookings`,
        `${this.apiUrl}/bookings?clubId=${encodeURIComponent(clubId)}`
      );
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.post<{
      id: string;
      tableId: string;
      bookingStart: string;
      bookingEnd: string;
      status: string;
      links: Record<string, string>;
    }>(
      url,
      { tableId, date }
    );
  }

  getReviews(id: string, page = 0, size = 10, links?: unknown): Observable<Review[]> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviews, `club ${id} reviews`, `${this.apiUrl}/reviews?clubId=${encodeURIComponent(id)}`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Review[]>(url, { params, observe: 'response' }).pipe(
      switchMap(response => this.collectionFromResponse(response, review => review))
    );
  }

  getReviewsPaginated(id: string, page = 0, size = 10, links?: unknown): Observable<any> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviews, `club ${id} reviews`, `${this.apiUrl}/reviews?clubId=${encodeURIComponent(id)}`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Review[]>(url, { params, observe: 'response' }).pipe(
      switchMap(response => this.collectionFromResponse(response, review => review)),
      map(content => ({ content, page, size }))
    );
  }

  getEvents(id: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.events, `club ${id} events`, `${this.apiUrl}/events?clubId=${encodeURIComponent(id)}`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Array<Event & { id?: string }>>(url, { observe: 'response' }).pipe(
      switchMap((response) => this.collectionFromResponse(response, (event) => this.normalizeEvent(event)))
    );
  }

  updateClub(clubId: string, payload: ClubUpdatePayload): Observable<Club> {
    return this.http.patch<Club>(`${this.apiUrl}/clubs/${clubId}`, payload).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  createProduct(clubId: string, payload: ProductPayload, links?: unknown): Observable<Product> {
    const url = this.resolveClubLink(links, this.linkRels.menu, `club ${clubId} menu`, `${this.apiUrl}/clubs/${clubId}/menu`);

    return this.http.post<Product>(url, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(map(product => this.normalizeProduct(product)));
  }

  updateProduct(clubId: string, productId: string, payload: ProductPayload, product?: Product): Observable<Product> {
    const url = this.resolveClubLink(
      product?.links,
      ['self', 'update'],
      `product ${productId}`,
      `${this.apiUrl}/clubs/${clubId}/menu/${productId}`
    );
    return this.http.put<Product>(url, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(map(updated => this.normalizeProduct(updated)));
  }

  deleteProduct(clubId: string, productId: string, product?: Product): Observable<void> {
    const url = this.resolveClubLink(
      product?.links,
      ['self', 'delete'],
      `product ${productId}`,
      `${this.apiUrl}/clubs/${clubId}/menu/${productId}`
    );
    return this.http.delete<void>(url);
  }

  createTable(clubId: string, payload: TablePayload, links?: unknown): Observable<ClubTable> {
    const url = this.resolveClubLink(links, this.linkRels.tables, `club ${clubId} tables`, `${this.apiUrl}/clubs/${clubId}/tables`);

    return this.http.post<ClubTable>(url, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(map(table => this.normalizeTable(table)));
  }

  updateTable(clubId: string, tableId: string, payload: TablePayload, table?: ClubTable): Observable<ClubTable> {
    const url = this.resolveClubLink(
      table?.links,
      ['self', 'update'],
      `table ${tableId}`,
      `${this.apiUrl}/clubs/${clubId}/tables/${tableId}`
    );
    return this.http.put<ClubTable>(url, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(map(updated => this.normalizeTable(updated)));
  }

  deleteTable(clubId: string, tableId: string, table?: ClubTable): Observable<void> {
    const url = this.resolveClubLink(
      table?.links,
      ['self', 'delete'],
      `table ${tableId}`,
      `${this.apiUrl}/clubs/${clubId}/tables/${tableId}`
    );
    return this.http.delete<void>(url);
  }

  createEvent(clubId: string, payload: EventPayload, links?: unknown): Observable<Event> {
    const url = this.resolveClubLink(links, this.linkRels.events, `club ${clubId} events`, `${this.apiUrl}/events?clubId=${encodeURIComponent(clubId)}`);

    return this.http.post<Event & { id?: string }>(url, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      map(event => this.normalizeEvent(event))
    );
  }

  updateEvent(clubId: string, eventId: string, payload: EventPayload, event?: Event): Observable<Event> {
    const url = this.resolveClubLink(
      event?.links,
      ['update', 'self'],
      `event ${eventId}`,
      `${this.apiUrl}/events/${eventId}`
    );
    return this.http.put<Event & { id?: string }>(url, payload, {
      context: new HttpContext().set(SKIP_API_ERROR_REDIRECT, true),
    }).pipe(
      map(updated => this.normalizeEvent(updated))
    );
  }

  deleteEvent(clubId: string, eventId: string, event?: Event): Observable<void> {
    const url = this.resolveClubLink(
      event?.links,
      ['delete', 'self'],
      `event ${eventId}`,
      `${this.apiUrl}/events/${eventId}`
    );
    return this.http.delete<void>(url);
  }

  uploadEventPhoto(clubId: string, eventId: string, file: File, event?: Event): Observable<Event> {
    let url = '';
    try {
      url = this.resolveClubLink(
        event?.links,
        ['photo'],
        `event ${eventId} photo`,
        `${this.apiUrl}/events/${eventId}/photo`
      );
    } catch (err) {
      return this.missingLinkError(err);
    }

    const formData = new FormData();
    formData.append('file', file);


    return this.http.put<Event & { id?: string }>(url, formData).pipe(
      map(updated => this.normalizeEvent(updated))
    );
  }

  deleteEventPhoto(clubId: string, eventId: string, event?: Event): Observable<void> {
    let url = '';
    try {
      url = this.resolveClubLink(
        event?.links,
        ['photo'],
        `event ${eventId} photo`,
        `${this.apiUrl}/events/${eventId}/photo`
      );
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.delete<void>(url);
  }

  private normalizePhoto(photo: ClubPhoto, clubId: string): ClubPhoto {
    const links = normalizeLinks(photo.links);
    const url = photo.url || links?.['self'] || (photo.photoId ? this.getPhotoUrl(photo.photoId) : '');

    return {
      ...photo,
      url: url ? this.toAbsoluteUrl(url, clubId) : '',
      links
    };
  }

  private photosFromResponse(
    response: HttpResponse<ClubPhoto[] | { photos?: ClubPhoto[] }>,
    clubId: string
  ): Observable<ClubPhoto[]> {
    const body = response.body;
    const itemLinks = itemLinksFromCollection(body);
    if (itemLinks) {
      return of(itemLinks.map(link => {
        const url = resolveApiUrl(link, this.apiUrl);
        const resourcePath = url.split(/[?#]/, 1)[0];
        const photoId = decodeURIComponent(resourcePath.split('/').filter(Boolean).pop() ?? '');
        return this.normalizePhoto({ photoId, url, links: { self: url } }, clubId);
      }));
    }

    const photos = Array.isArray(body) ? body : (body?.photos ?? []);
    return of(photos
      .map(photo => this.normalizePhoto(photo, clubId))
      .filter(photo => !!photo.url));
  }

  private clubPageFromResponse(
    response: HttpResponse<Club[] | PagedResponse<Club>>,
    page: number,
    size: number
  ): Observable<PagedResponse<Club>> {
    const body = response.body;
    const itemLinks = itemLinksFromCollection(body);
    const total = totalFromHeader(response.headers.get('X-Total-Count'), itemLinks?.length ?? (body as PagedResponse<Club>)?.totalElements ?? 0);
    const base = {
      totalElements: total,
      totalPages: Math.ceil(total / size),
      page,
      size,
    };

    if (itemLinks) {
      const requests = itemLinks.map(link => this.getClubByLink(link));
      return (requests.length ? forkJoin(requests) : of([] as Club[])).pipe(
        map(content => ({ ...base, content }))
      );
    }

    const content = Array.isArray(body)
      ? this.normalizeClubs(body)
      : this.normalizeClubs((body as PagedResponse<Club> | null)?.content ?? []);

    return of({
      ...base,
      content,
      totalElements: (body as PagedResponse<Club> | null)?.totalElements ?? base.totalElements,
      totalPages: (body as PagedResponse<Club> | null)?.totalPages ?? base.totalPages,
    });
  }

  private collectionFromResponse<T>(
    response: HttpResponse<T[]>,
    normalize: (item: T) => T
  ): Observable<T[]> {
    const body = response.body;
    const itemLinks = itemLinksFromCollection(body);
    if (itemLinks) {
      const requests = itemLinks.map(link => this.http.get<T>(resolveApiUrl(link, this.apiUrl)).pipe(map(normalize)));
      return requests.length ? forkJoin(requests) : of([] as T[]);
    }
    return of((body ?? []).map(normalize));
  }
}
