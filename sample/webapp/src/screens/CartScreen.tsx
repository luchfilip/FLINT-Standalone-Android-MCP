import { useNavigate } from "react-router-dom";
import {
  useFlintScreen,
  useFlintTools,
  useFlintList,
  FlintText,
  FlintAction,
  FlintItem,
} from "flint-web";
import { useCart } from "../CartContext";

export function CartScreen() {
  const navigate = useNavigate();
  const { items, removeItem, total } = useCart();

  useFlintScreen("cart");
  useFlintList("cart_items", "Items in the shopping cart");

  useFlintTools([
    {
      name: "go_back",
      description: "Go back to the store",
      action: () => navigate("/"),
    },
  ]);

  return (
    <div>
      <FlintText flintKey="heading" className="text-2xl font-bold mb-2">
        Shopping Cart
      </FlintText>
      <FlintText flintKey="totalPrice" className="text-lg text-green-600 mb-6">
        {`Total: $${total.toFixed(2)}`}
      </FlintText>
      {items.length === 0 ? (
        <FlintText flintKey="emptyMessage" className="text-gray-400">
          Your cart is empty
        </FlintText>
      ) : (
        <div className="grid gap-3">
          {items.map((item, index) => (
            <FlintItem list="cart_items" index={index} key={item.product.id}>
              <div className="bg-white rounded-lg shadow p-4 flex justify-between items-center">
                <div>
                  <FlintText flintKey="name" className="font-semibold">
                    {item.product.name}
                  </FlintText>
                  <FlintText flintKey="quantity" className="text-sm text-gray-500">
                    {`Qty: ${item.quantity}`}
                  </FlintText>
                  <FlintText flintKey="subtotal" className="text-green-600">
                    {`$${(item.product.price * item.quantity).toFixed(2)}`}
                  </FlintText>
                </div>
                <FlintAction
                  flintName="remove"
                  flintDescription="Remove item from cart"
                  onClick={() => removeItem(item.product.id)}
                  className="px-3 py-1 bg-red-500 text-white rounded hover:bg-red-600"
                >
                  Remove
                </FlintAction>
              </div>
            </FlintItem>
          ))}
        </div>
      )}
    </div>
  );
}
