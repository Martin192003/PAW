import { Routes } from '@angular/router';
import { ClubsPage } from './pages/clubs.page';
import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'clubs',
    pathMatch: 'full'
  },
  {
    path: 'clubs',
    component: ClubsPage
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login.page').then(m => m.LoginPage)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register.page').then(m => m.RegisterPage)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./pages/change-password.page').then(m => m.ChangePasswordPage)
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./pages/change-password.page').then(m => m.ChangePasswordPage)
  },
  {
    path: 'verify-mail',
    loadComponent: () => import('./pages/verify-mail.page').then(m => m.VerifyMailPage)
  },
  {
    path: 'verify-account',
    loadComponent: () => import('./pages/verify-mail.page').then(m => m.VerifyMailPage)
  },
  {
    path: 'clubs/new',
    loadComponent: () => import('./pages/club-new.page').then(m => m.AddClubComponent),
    canActivate: [authGuard]
  },
  {
    path: 'clubs/handle',
    loadComponent: () => import('./pages/club-handler.page').then(m => m.ClubHandlerPage),
    canActivate: [adminGuard]
  },
  {
    path: 'clubs/:id',
    loadComponent: () =>
      import('./pages/club-details.page')
        .then(m => m.ClubDetailsPage)
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile.page').then(m => m.ProfilePage)
  },
  {
    path: 'profile/reviews',
    loadComponent: () => import('./pages/my-reviews.page').then(m => m.MyReviewsPage)
  },
  {
    path: 'admin/activity',
    loadComponent: () => import('./pages/club-activity.page').then(m => m.ClubActivityPage),
    canActivate: [adminGuard]
  },
  {
    path: 'admin/bookings',
    loadComponent: () => import('./pages/managed-club-bookings.page').then(m => m.ManagedClubBookingsPage),
    canActivate: [adminGuard]
  },
  {
    path: 'admin/bookings/:bookingId',
    loadComponent: () => import('./pages/booking-admin-handler.page').then(m => m.BookingAdminHandlerPage),
    canActivate: [adminGuard]
  },
  {
    path: 'bookings',
    loadComponent: () => import('./pages/user-bookings.page').then(m => m.UserBookingsPage)
  },
  {
    path: 'bookings/:clubId/:bookingId',
    loadComponent: () => import('./pages/booking-user-handler.page').then(m => m.BookingUserHandlerPage)
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./pages/forbidden.page').then(m => m.ForbiddenPage)
  },
  {
    path: 'error',
    loadComponent: () => import('./pages/error.page').then(m => m.ErrorPage)
  },
  {
    path: 'not-found',
    loadComponent: () => import('./pages/not-found.page').then(m => m.NotFoundPage)
  },
  {
    path: '**',
    redirectTo: 'not-found'
  }
];
