import { TestBed } from '@angular/core/testing';
import { PhotoService } from './photo.service';

describe('PhotoService', () => {
  let service: PhotoService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [PhotoService] });
    service = TestBed.inject(PhotoService);
  });

  it('builds a photo URL under /photos/{photoId}', () => {
    const url = service.getPhotoUrl('photo-123');

    expect(url).toContain('/photos/photo-123');
  });

  it('produces a different URL for a different photoId', () => {
    expect(service.getPhotoUrl('a')).not.toBe(service.getPhotoUrl('b'));
  });
});
