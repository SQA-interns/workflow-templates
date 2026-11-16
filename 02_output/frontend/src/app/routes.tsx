import type { RouteObject } from "react-router";
import { AppLayout } from "./AppLayout";
import { HomePage, NotFoundPage } from "./pages";

/** Route table (specification, "User interface"); each story adds its routes here. */
export const routes: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
];
