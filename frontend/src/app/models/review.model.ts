export interface Review {
  reviewId: string;
  userId?: string;
  clubId?: string;
  rating: number;
  description?: string;
  createdAt: string;
  fromTableGuest?: boolean;
  photoId?: string;
  photoUrl?: string;
  user?: {
    username: string;
  };
  username?: string;
}
