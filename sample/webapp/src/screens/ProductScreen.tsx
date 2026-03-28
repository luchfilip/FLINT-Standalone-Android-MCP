import { useParams, useNavigate } from "react-router-dom";
import {
  useFlintScreen,
  useFlintTools,
  FlintText,
  FlintAction,
} from "flint-web";
import { products } from "../data";
import { useCart } from "../CartContext";

export function ProductScreen() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { addItem } = useCart();
  const product = products.find((p) => p.id === id);

  useFlintScreen("product_detail");

  useFlintTools([
    {
      name: "go_back",
      description: "Go back to the store",
      action: () => navigate("/"),
    },
    {
      name: "add_to_cart",
      description: "Add this product to the cart",
      action: () => { if (product) addItem(product); },
    },
  ]);

  if (!product) {
    return <FlintText flintKey="error" className="text-red-500">Product not found</FlintText>;
  }

  return (
    <div className="bg-white rounded-lg shadow p-6 max-w-lg">
      <FlintText flintKey="name" className="text-2xl font-bold mb-2">
        {product.name}
      </FlintText>
      <FlintText flintKey="price" className="text-xl text-green-600 mb-2">
        {`$${product.price.toFixed(2)}`}
      </FlintText>
      <FlintText flintKey="category" className="text-sm text-gray-400 mb-4">
        {product.category}
      </FlintText>
      <FlintText flintKey="description" className="text-gray-700 mb-6">
        {product.description}
      </FlintText>
      <div className="flex gap-3">
        <FlintAction
          flintName="add_to_cart"
          flintDescription="Add to cart"
          onClick={() => addItem(product)}
          className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
        >
          Add to Cart
        </FlintAction>
        <FlintAction
          flintName="go_back"
          flintDescription="Back to store"
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300"
        >
          Back
        </FlintAction>
      </div>
    </div>
  );
}
