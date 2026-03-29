export interface Review {
  reviewId: string;
  rating: number;
  description: string;
  createdAt: string;
  user: {
    username: string;
  };
}
