import { of, throwError, BehaviorSubject } from 'rxjs';
import { vi } from 'vitest';
import { convertToParamMap, ParamMap } from '@angular/router';
import { ClubActivityPage } from './club-activity.page';

describe('ClubActivityPage', () => {
  let clubService: { getClubsByUser: ReturnType<typeof vi.fn> };
  let authState: { getUser: ReturnType<typeof vi.fn> };
  let cdr: { detectChanges: ReturnType<typeof vi.fn> };
  let router: { navigate: ReturnType<typeof vi.fn> };
  let queryParamMap$: BehaviorSubject<ParamMap>;
  let route: { snapshot: { queryParamMap: ParamMap }; queryParamMap: BehaviorSubject<ParamMap> };
  let component: ClubActivityPage;

  const club = {
    clubId: 'club-1',
    clubName: 'Club One',
    description: 'A club',
    location: 'Palermo',
    zone: 'ZONA_CABA',
    musicGenre: 'TECHNO',
    priceRange: 'MEDIO',
    bar: false,
    cbu: '123',
    rating: 4.5,
    quantityRatings: 12,
    createdAt: '2026-07-03T12:00:00Z',
    links: { cover: '/api/clubs/club-1/cover' },
  } as any;

  beforeEach(() => {
    clubService = { getClubsByUser: vi.fn() };
    authState = { getUser: vi.fn().mockReturnValue({ userId: 'admin-1' }) };
    cdr = { detectChanges: vi.fn() };


    queryParamMap$ = new BehaviorSubject<ParamMap>(convertToParamMap({}));
    route = {
      snapshot: { queryParamMap: convertToParamMap({}) },
      queryParamMap: queryParamMap$
    };
    router = {
      navigate: vi.fn().mockImplementation((_commands: unknown[], extras: { queryParams?: Record<string, unknown> }) => {
        queryParamMap$.next(convertToParamMap(extras?.queryParams ?? {}));
        return Promise.resolve(true);
      })
    };

    clubService.getClubsByUser.mockImplementation(
      (_userId, page) => of({ content: [club], page, totalPages: 3 })
    );

    component = new ClubActivityPage(
      clubService as any,
      authState as any,
      cdr as any,
      router as any,
      route as any
    );
  });

  it('loads the admin activity view', () => {
    component.ngOnInit();

    expect(clubService.getClubsByUser).toHaveBeenCalledWith('admin-1', 1, 3);
    expect(component.clubs).toEqual([expect.objectContaining({
      clubId: 'club-1',
      _hasError: false,
    })]);
    expect(component.totalPages).toBe(3);
  });

  it('keeps an empty state when the admin has no clubs', () => {
    clubService.getClubsByUser.mockReturnValueOnce(of({ content: [], page: 1, totalPages: 0 }));

    component.ngOnInit();

    expect(component.clubs).toEqual([]);
    expect(component.totalPages).toBe(0);
  });

  it('paginates the admin activity list within bounds', () => {
    component.ngOnInit();

    component.nextPage();
    expect(component.currentPage).toBe(2);
    expect(clubService.getClubsByUser).toHaveBeenLastCalledWith('admin-1', 2, 3);

    component.prevPage();
    expect(component.currentPage).toBe(1);
    expect(clubService.getClubsByUser).toHaveBeenLastCalledWith('admin-1', 1, 3);
  });

  it('navigates to the selected club handler', () => {
    component.goToClubHandler('club-1');

    expect(router.navigate).toHaveBeenCalledWith(['/clubs/handle'], {
      queryParams: { clubId: 'club-1' },
    });
  });

  it('clears the list when loading admin clubs fails', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    clubService.getClubsByUser.mockReturnValueOnce(throwError(() => new Error('boom')));

    component.ngOnInit();

    expect(component.clubs).toEqual([]);
    expect(component.totalPages).toBe(0);
    consoleError.mockRestore();
  });
});
