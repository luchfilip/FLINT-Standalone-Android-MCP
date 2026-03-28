import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { FlintProvider, FlintRouter } from "flint-web";
import { CartProvider } from "./CartContext";
import App from "./App";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <FlintProvider>
      <CartProvider>
        <BrowserRouter>
          <FlintRouter>
            <App />
          </FlintRouter>
        </BrowserRouter>
      </CartProvider>
    </FlintProvider>
  </React.StrictMode>
);
