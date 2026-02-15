import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class PhotoService {

  private baseUrl = 'http://localhost:8080/webapp';

  getPhotoUrl(photoId: number): string {
    return `${this.baseUrl}/photos/${photoId}`;
  }
}
