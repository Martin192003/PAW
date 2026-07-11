export interface Product {
  productId: string;
  productName: string;
  description: string;
  basePrice: number;
  productType: string;
  isAvailable: boolean;
  links?: Record<string, string>;
}
