import { useNavigate } from "react-router-dom";
import {
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintText,
  FlintAction,
  FlintItem,
} from "flint-web";
import { products } from "../data";
import { useCart } from "../CartContext";

export function HomeScreen() {
  const navigate = useNavigate();
  const { addItem, items } = useCart();

  useFlintScreen("home");
  useFlintList("products", "Available products in the store");

  useFlintTools([
    {
      name: "view_product",
      description: "View product details",
      params: [
        { name: "id", type: "string", description: "Product ID", required: true },
      ],
      action: (params) => navigate(`/product/${params.id}`),
    },
    {
      name: "go_to_cart",
      description: "Navigate to the shopping cart",
      action: () => navigate("/cart"),
    },
  ]);

  return (
    <div>
      <FlintText flintKey="heading" className="text-2xl font-bold mb-2">
        Sample Store
      </FlintText>
      <FlintText flintKey="cartSummary" className="text-sm text-gray-500 mb-6">
        {`${items.length} item(s) in cart`}
      </FlintText>
      <div className="grid gap-4">
        {products.map((product, index) => (
          <FlintItem list="products" index={index} key={product.id}>
            <div className="bg-white rounded-lg shadow p-4 flex justify-between items-center">
              <div>
                <FlintText flintKey="name" className="font-semibold text-lg">
                  {product.name}
                </FlintText>
                <FlintText flintKey="price" className="text-green-600">
                  {`$${product.price.toFixed(2)}`}
                </FlintText>
                <FlintText flintKey="category" className="text-sm text-gray-400">
                  {product.category}
                </FlintText>
              </div>
              <div className="flex gap-2">
                <FlintAction
                  flintName="view"
                  flintDescription="View product details"
                  onClick={() => navigate(`/product/${product.id}`)}
                  className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
                >
                  View
                </FlintAction>
                <FlintAction
                  flintName="add_to_cart"
                  flintDescription="Add product to cart"
                  onClick={() => addItem(product)}
                  className="px-3 py-1 bg-green-500 text-white rounded hover:bg-green-600"
                >
                  Add to Cart
                </FlintAction>
              </div>
            </div>
          </FlintItem>
        ))}
      </div>
    </div>
  );
}
