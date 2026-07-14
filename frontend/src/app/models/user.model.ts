import { Link } from './club.model';

export interface UserCreateRequest {
  username: string;
  name: string;
  surname: string;
  email: string;
  password: string;
  cellphone: string;
  locale: string;
}

export interface UserUpdateRequest {
  username?: string;
  name?: string;
  surname?: string;
  email?: string;
  cellphone?: string;
  locale?: string;
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

export interface PublicUser {
  userId: string;
  username: string;
  name: string;
  surname: string;
  links?: Link[];
}

export interface User extends PublicUser {
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
