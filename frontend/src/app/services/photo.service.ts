import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PhotoService {

  private baseUrl = 'http://localhost:8080/webapp/api';

  getPhotoUrl(photoId: string): string {
    return `${this.baseUrl}/photos/${photoId}`;
  }
}
