export interface Club {
  clubId: string;
  clubName: string;
  description: string;
  location: string;
  zone: string;
  musicGenre: string;
  priceRange: string;
  bar: boolean;
  cbu: string;
  rating: number;
  quantityRatings: number;
  createdAt: string;

  links?: {
    self: string;
    cover: string;
    layout: string;
    limits: string;
  };
}
