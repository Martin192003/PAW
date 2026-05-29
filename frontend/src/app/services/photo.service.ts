import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { resolveApiUrl } from '../utils/api-url.util';

@Injectable({ providedIn: 'root' })
export class PhotoService {

  private baseUrl = environment.apiBaseUrl;

  getPhotoUrl(photoId: string): string {
    return resolveApiUrl(`${this.baseUrl}/photos/${photoId}`, this.baseUrl);
  }
}
