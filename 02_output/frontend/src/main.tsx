import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router";
import { retryUnlessClientError } from "./api/retry";
import { routes } from "./app/routes";
import { AuthProvider } from "./auth/AuthProvider";
import "./i18n";
import "./styles/index.css";

// A 4xx answer is final (not found, forbidden, invalid): show it at once; retry the rest.
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: retryUnlessClientError } },
});
const router = createBrowserRouter(routes);
const root = document.getElementById("root");

if (root) {
  createRoot(root).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <RouterProvider router={router} />
        </AuthProvider>
      </QueryClientProvider>
    </StrictMode>,
  );
}
