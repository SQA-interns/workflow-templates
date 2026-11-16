/** A Group of the signed-in User with the User's Role (`Group` of the contract). */
export interface Group {
  id: string;
  name: string;
  role: "OWNER" | "EDITOR" | "VIEWER";
}
