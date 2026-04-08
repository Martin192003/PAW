export interface UserCreateRequest {
  username: string;
  name: string;
  surname: string;
  email: string;
  password: string;
  cellphone: string;
  locale: string;
}

export interface UserLoginRequest {
  usernameOrEmail: string;
  password: string;
}

export interface UserVerificationRequest {
  token?: string;
  code?: string;
}

export interface ResendVerificationRequest {
  usernameOrEmail: string;
}

export interface ApiMessageResponse {
  message: string;
}

export interface User {
  userId: string;
  username: string;
  name: string;
  surname: string;
  email: string;
  cellphone: string;
  role: string;
  verified: boolean;
  createdAt: string;
  locale: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}
