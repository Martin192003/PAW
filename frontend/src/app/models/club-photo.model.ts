import { ClubLinks } from './club.model';

export interface ClubPhoto {
  photoId?: string;
  url: string;
  photoType?: string;
  links?: ClubLinks;
}

