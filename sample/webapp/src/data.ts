export type Product = {
  id: string;
  name: string;
  price: number;
  description: string;
  category: string;
};

export const products: Product[] = [
  { id: "1", name: "Wireless Headphones", price: 79.99, description: "Noise-cancelling over-ear headphones", category: "Electronics" },
  { id: "2", name: "Running Shoes", price: 129.99, description: "Lightweight trail running shoes", category: "Sports" },
  { id: "3", name: "Coffee Maker", price: 49.99, description: "12-cup drip coffee maker", category: "Kitchen" },
  { id: "4", name: "Backpack", price: 59.99, description: "Water-resistant laptop backpack", category: "Accessories" },
  { id: "5", name: "Desk Lamp", price: 34.99, description: "LED desk lamp with adjustable brightness", category: "Office" },
];

export type CartItem = { product: Product; quantity: number };
