import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, forkJoin, map, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AvailabilityPagedResponse } from '../models/availability-paged-response.model';
import { ClubLimits } from '../models/club-limits.model';
import { ClubPhoto } from '../models/club-photo.model';
import { ClubTable } from '../models/club-table.model';
import { Club, ClubPayload } from '../models/club.model';
import { Event } from '../models/event.model';
import { Layout } from '../models/layout.model';
import { PagedResponse } from '../models/paged-response.model';
import { Product } from '../models/product.model';
import { Review } from '../models/review.model';
import { normalizeLinks, requireAnyLink, resolveApiUrl } from '../utils/api-url.util';
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

  /**
   * Resuelve la URL de un recurso a partir de los links HATEOAS del club.
   *
   * La construcción manual (`fallback`) es un último recurso **documentado**:
   * solo se usa cuando todavía no tenemos el objeto con `links` a mano (por
   * ejemplo, la primera carga de una página a partir de un id de ruta). En
   * cuanto el llamador tiene el recurso padre ya cargado, debe pasar sus
   * `links` para que se use el href que devuelve el backend en vez de la URL
   * construida a mano.
   */
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
  ): Observable<AvailabilityPagedResponse<Club>> {
    const normalized = { ...this.emptyFilter, ...filters };

    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (normalized.name) params = params.set('name', normalized.name);
    if (normalized.zone) params = params.set('zone', normalized.zone);
    if (normalized.genre) params = params.set('genre', normalized.genre);
    if (normalized.price) params = params.set('price', normalized.price);
    if (normalized.isBar) params = params.set('isBar', normalized.isBar);
    if (normalized.sort) params = params.set('sort', normalized.sort);

    return this.http.get<AvailabilityPagedResponse<Club>>(`${this.apiUrl}/clubs`, { params }).pipe(
      map(response => ({
        ...response,
        content: this.normalizeClubs(response.content ?? [])
      }))
    );
  }

  getClubsByUser(userId: string, page: number, size: number): Observable<PagedResponse<Club>> {
    const params = new HttpParams()
      .set('ownerId', userId)
      .set('page', page)
      .set('size', size);

    return this.http.get<PagedResponse<Club>>(
      `${this.apiUrl}/clubs`,
      { params }
    ).pipe(
      map(response => ({
        ...response,
        content: this.normalizeClubs(response.content ?? [])
      }))
    );
  }

  createClub(payload: ClubPayload): Observable<Club> {
    return this.http.post<Club>(`${this.apiUrl}/clubs`, payload).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  /**
   * Carga un club a partir de su id.
   *
   * Este es el único punto de entrada legítimo por id: se usa cuando todavía
   * no tenemos ningún recurso con links (por ejemplo, al entrar directo a
   * `/clubs/:id` desde la URL). Si ya tenés un club, booking, producto, etc.
   * con `links.club`, preferí `getClubByLink` en su lugar.
   */
  getClubById(id: string): Observable<Club> {
    return this.http.get<Club>(`${this.apiUrl}/clubs/${id}`).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  getClubByLink(link: string): Observable<Club> {
    return this.http.get<Club>(resolveApiUrl(link, this.apiUrl)).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  /**
   * Fallback documentado para la URL de portada cuando todavía no tenemos el
   * club cargado (y por lo tanto no tenemos `club.links['cover']`). Los
   * llamadores que ya tienen el club deben preferir su link `cover`.
   */
  getCoverUrl(id: string): string {
    return resolveApiUrl(`${this.apiUrl}/clubs/${id}/cover`, this.apiUrl);
  }

  /**
   * Fallback documentado para la URL de una foto cuando no tenemos el link
   * `self` de la foto a mano. Preferir `photo.links['self']` cuando exista.
   */
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
        context: this.optionalApiContext()
      })
      .pipe(
        map(response => Array.isArray(response) ? response : (response?.photos ?? [])),
        map(photos => photos
          .map(photo => this.normalizePhoto(photo, id))
          .filter(photo => !!photo.url)
        )
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
        context: this.optionalApiContext()
      })
      .pipe(
        map(response => Array.isArray(response) ? response : (response?.photos ?? [])),
        map(photos => photos
          .map(photo => this.normalizePhoto(photo, id))
          .filter(photo => !!photo.url)
        )
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

  /**
   * No hay un link HATEOAS individual por foto para borrar (el backend solo
   * expone `self` de la foto como GET en /photos/{id}). El recurso que sí
   * soporta DELETE es la colección `photos` del club, así que resolvemos esa
   * URL a partir de los links del club y le agregamos el id.
   */
  deleteGalleryPhoto(clubId: string, photoId: string, links?: unknown): Observable<void> {
    let base = '';
    try {
      base = this.resolveClubLink(links, this.linkRels.photos, `club ${clubId} photos`, `${this.apiUrl}/clubs/${clubId}/photos`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.delete<void>(`${base}/${encodeURIComponent(photoId)}`);
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

    return this.http.get<Product[]>(url).pipe(
      map(products => (products ?? []).map(product => this.normalizeProduct(product)))
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

    return this.http.get<ClubTable[]>(url, { params }).pipe(
      map(tables => (tables ?? []).map(table => this.normalizeTable(table)))
    );
  }

  reserveTable(clubId: string, tableId: string, date: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(
        links,
        this.linkRels.bookings,
        `club ${clubId} bookings`,
        `${this.apiUrl}/clubs/${encodeURIComponent(clubId)}/bookings`
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
      url = this.resolveClubLink(links, this.linkRels.reviews, `club ${id} reviews`, `${this.apiUrl}/clubs/${id}/reviews`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(url, { params }).pipe(
      map(response => Array.isArray(response) ? response : (response?.content ?? []))
    );
  }

  getReviewsPaginated(id: string, page = 0, size = 10, links?: unknown): Observable<any> {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.reviews, `club ${id} reviews`, `${this.apiUrl}/clubs/${id}/reviews`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(url, { params });
  }

  getEvents(id: string, links?: unknown) {
    let url = '';
    try {
      url = this.resolveClubLink(links, this.linkRels.events, `club ${id} events`, `${this.apiUrl}/clubs/${id}/events`);
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.get<Array<Event & { id?: string }>>(url).pipe(
      map((events) =>
        (events ?? []).map((event) => ({
          ...event,
          eventId: event.eventId || event.id || '',
          links: normalizeLinks(event.links)
        }))
      )
    );
  }

  updateClub(clubId: string, payload: ClubUpdatePayload): Observable<Club> {
    return this.http.put<Club>(`${this.apiUrl}/clubs/${clubId}`, payload).pipe(
      map(club => this.normalizeClub(club))
    );
  }

  createProduct(clubId: string, payload: ProductPayload, links?: unknown): Observable<Product> {
    const url = this.resolveClubLink(links, this.linkRels.menu, `club ${clubId} menu`, `${this.apiUrl}/clubs/${clubId}/menu`);
    return this.http.post<Product>(url, payload).pipe(map(product => this.normalizeProduct(product)));
  }

  updateProduct(clubId: string, productId: string, payload: ProductPayload, product?: Product): Observable<Product> {
    const url = this.resolveClubLink(
      product?.links,
      ['self', 'update'],
      `product ${productId}`,
      `${this.apiUrl}/clubs/${clubId}/menu/${productId}`
    );
    return this.http.put<Product>(url, payload).pipe(map(updated => this.normalizeProduct(updated)));
  }

  createTable(clubId: string, payload: TablePayload, links?: unknown): Observable<ClubTable> {
    const url = this.resolveClubLink(links, this.linkRels.tables, `club ${clubId} tables`, `${this.apiUrl}/clubs/${clubId}/tables`);
    // El 409 por numero de mesa duplicado lo maneja club-handler.page.ts como
    // error de campo (igual que el resto de los formularios); sin este flag
    // el interceptor global lo agarra primero y redirige a /error.
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
    const url = this.resolveClubLink(links, this.linkRels.events, `club ${clubId} events`, `${this.apiUrl}/clubs/${clubId}/events`);
    return this.http.post<Event>(url, payload).pipe(
      map(event => ({ ...event, links: normalizeLinks(event.links) }))
    );
  }

  updateEvent(clubId: string, eventId: string, payload: EventPayload, event?: Event): Observable<Event> {
    const url = this.resolveClubLink(
      event?.links,
      ['update', 'self'],
      `event ${eventId}`,
      `${this.apiUrl}/clubs/${clubId}/events/${eventId}`
    );
    return this.http.put<Event>(url, payload).pipe(
      map(updated => ({ ...updated, links: normalizeLinks(updated.links) }))
    );
  }

  deleteEvent(clubId: string, eventId: string, event?: Event): Observable<void> {
    const url = this.resolveClubLink(
      event?.links,
      ['delete', 'self'],
      `event ${eventId}`,
      `${this.apiUrl}/clubs/${clubId}/events/${eventId}`
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
        `${this.apiUrl}/clubs/${clubId}/events/${eventId}/photo`
      );
    } catch (err) {
      return this.missingLinkError(err);
    }

    const formData = new FormData();
    formData.append('file', file);

    return this.http.put<Event>(url, formData);
  }

  deleteEventPhoto(clubId: string, eventId: string, event?: Event): Observable<Event> {
    let url = '';
    try {
      url = this.resolveClubLink(
        event?.links,
        ['photo'],
        `event ${eventId} photo`,
        `${this.apiUrl}/clubs/${clubId}/events/${eventId}/photo`
      );
    } catch (err) {
      return this.missingLinkError(err);
    }

    return this.http.delete<Event>(url);
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
}
