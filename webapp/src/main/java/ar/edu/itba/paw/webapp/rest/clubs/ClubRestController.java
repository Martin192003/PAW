package ar.edu.itba.paw.webapp.rest.clubs;

import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.dto.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.auth.PawUserDetails;
import ar.edu.itba.paw.webapp.rest.LinkBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Path("/clubs")
@Produces(MediaType.APPLICATION_JSON)
public class ClubRestController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final ZoneId BUENOS_AIRES_ZONE = ZoneId.of("America/Argentina/Buenos_Aires");

    private final ClubService clubService;
    private final TableService tableService;
    private final UserService userService;
    private final ClubLayoutService clubLayoutService;
    private final PhotoService photoService;
    private final PhotoDtoAssembler photoDtoAssembler;


    @Context
    private UriInfo uriInfo;

    @Context
    private HttpServletRequest request;

    @Autowired
    public ClubRestController(final ClubService clubService,
                              final TableService tableService,
                              UserService userService,
                              ClubLayoutService clubLayoutService,
                              PhotoService photoService,
                              PhotoDtoAssembler photoDtoAssembler) {
        this.clubService = clubService;
        this.tableService = tableService;
        this.userService = userService;
        this.clubLayoutService = clubLayoutService;
        this.photoService = photoService;
        this.photoDtoAssembler = photoDtoAssembler;
    }

    @GET
    public Response listClubs(@QueryParam("name") final String nameLike,
                              @QueryParam("ownerId") final String ownerId,
                              @QueryParam("zone") final Zone zone,
                              @QueryParam("genre") final String musicGenre,
                              @QueryParam("price") final String priceRange,
                              @QueryParam("isBar") final Boolean isBar,
                              @QueryParam("availableToday") final Boolean availableToday,
                              @QueryParam("sort") final String sort,
                              @QueryParam("page") @DefaultValue("1") int page,
                              @QueryParam("size") @DefaultValue("10") int size) {

        page = (page < 1) ? DEFAULT_PAGE : page;
        size = (size < 1) ? DEFAULT_SIZE : size;

        if (ownerId != null && !ownerId.isBlank()) {
            return listClubsByOwner(ownerId, page, size);
        }

        final String normalizedPriceRange =
                (priceRange == null || priceRange.isBlank()) ? null : priceRange.toUpperCase();

        final boolean availabilityMode = isAvailabilityMode(availableToday, sort);
        final LocalDate selectedDay = resolveSelectedDay(availabilityMode, sort);
        final boolean invalidDay = availabilityMode && !tableService.isAllowedDay(selectedDay);

        int totalClubs = 0;
        List<Club> clubs = Collections.emptyList();
        final int offset = (page - 1) * size;

        if (availabilityMode) {
            if (!invalidDay) {
                totalClubs = clubService.countSearchAvailable(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, selectedDay
                );
                clubs = clubService.searchAvailablePaginated(
                        nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, selectedDay, size, offset
                );
            }
        } else {
            totalClubs = clubService.countSearch(nameLike, zone, musicGenre, normalizedPriceRange, isBar);
            clubs = clubService.searchPaginated(
                    nameLike, zone, musicGenre, normalizedPriceRange, isBar, sort, size, offset
            );
        }

        final int totalPages = (int) Math.ceil((double) totalClubs / size);

        List<ClubDto> dtos = clubs.stream()
                .map(this::toDtoWithLinks)
                .toList();


        PagedResponseDto response = new PagedResponseDto(
                dtos,
                page,
                size,
                totalClubs,
                totalPages,
                availabilityMode,
                invalidDay,
                selectedDay != null ? selectedDay.toString() : null
        );

        Response.ResponseBuilder responseBuilder = Response.ok(response)
                .header("X-Total-Count", totalClubs)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");
        addPaginationLinks(responseBuilder, page, size, totalPages, 1);
        return responseBuilder.build();
    }

    private Response listClubsByOwner(final String ownerId, final int page, final int size) {
        UUID id = parseUuidOr400(ownerId);
        if (id == null) return badRequest("ownerId invalido");

        final int offset = (page - 1) * size;
        final int totalClubs = clubService.countClubsByUser(id);
        final List<ClubDto> dtos = clubService.getClubsByUserPaginated(id, size, offset)
                .stream()
                .map(this::toDtoWithLinks)
                .collect(Collectors.toList());

        final int totalPages = (int) Math.ceil((double) totalClubs / size);
        final PagedResponseDto response = new PagedResponseDto(
                dtos,
                page,
                size,
                totalClubs,
                totalPages,
                false,
                false,
                null
        );

        Response.ResponseBuilder responseBuilder = Response.ok(response)
                .header("X-Total-Count", totalClubs)
                .header("Cache-Control", "no-cache, no-store, must-revalidate")
                .header("Pragma", "no-cache")
                .header("Expires", "0");
        addPaginationLinks(responseBuilder, page, size, totalPages, 1);
        return responseBuilder.build();
    }

    private boolean isAvailabilityMode(final Boolean availableToday, final String sort) {
        return (availableToday != null && availableToday)
                || (sort != null && sort.startsWith("availability_"));
    }

    private LocalDate resolveSelectedDay(final boolean availabilityMode, final String sort) {
        if (!availabilityMode) {
            return null;
        }
        final Integer dayOffset = extractDayOffsetFromSort(sort);
        return (dayOffset != null)
                ? LocalDate.now(BUENOS_AIRES_ZONE).plusDays(dayOffset)
                : LocalDate.now(BUENOS_AIRES_ZONE);
    }

    private Integer extractDayOffsetFromSort(final String sort) {
        if (sort == null) {
            return null;
        }
        final Pattern pattern = Pattern.compile("availability_d([0-2])_(?:asc|desc)");
        final Matcher matcher = pattern.matcher(sort);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return null;
    }

    @GET
    @Path("/{clubId}")
    public Response getClub(@PathParam("clubId") String clubId) {
        UUID id;
        try {
            id = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        Club club = clubService.getClubById(id).orElse(null);
        if (club == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        return Response.ok(toDtoWithLinks(club)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClub(ClubDto body,
                               @Context SecurityContext securityContext) {

        if (body == null ||
                isBlank(body.getClubName()) ||
                isBlank(body.getLocation()) ||
                isBlank(body.getDescription()) ||
                isBlank(body.getZone()) ||
                isBlank(body.getMusicGenre()) ||
                isBlank(body.getPriceRange()) ||
                isBlank(body.getCbu())) {

            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Faltan campos requeridos"))
                    .build();
        }

        User user = resolveAuthenticatedUser(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        Club created = clubService.create(
                body.getClubName(),
                body.getDescription(),
                body.getLocation(),
                Price.valueOf(body.getPriceRange()),
                user,
                MusicalGenre.valueOf(body.getMusicGenre()),
                Zone.valueOf(body.getZone()),
                body.isBar(),
                body.getCbu(),
                null
        );

        URI uri = uriInfo.getAbsolutePathBuilder()
                .path(created.getClubId().toString())
                .build();

        return Response.created(uri)
                .entity(toDtoWithLinks(created))
                .build();
    }

    @PUT
    @Path("/{clubId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateClub(@PathParam("clubId") String clubId, ClubDto body, @Context SecurityContext securityContext) {
        UUID id;
        try {
            id = UUID.fromString(clubId);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "clubId inválido"))
                    .build();
        }

        Optional<Club> existingOpt = clubService.getClubById(id);
        if (existingOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", "Club no encontrado"))
                    .build();
        }

        if (body == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", "Body requerido"))
                    .build();
        }

        Club existing = existingOpt.get();
        User user = resolveAuthenticatedUser(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        clubService.assertAdmin(id, user.getId());

        Price price = body.getPriceRange() != null
                ? Price.valueOf(body.getPriceRange())
                : existing.getPriceRange();

        MusicalGenre genre = body.getMusicGenre() != null
                ? MusicalGenre.valueOf(body.getMusicGenre())
                : existing.getMusicGenre();

        Zone zone = body.getZone() != null
                ? Zone.valueOf(body.getZone())
                : existing.getZone();

        String name = body.getClubName() != null ? body.getClubName() : existing.getClubName();
        String description = body.getDescription() != null ? body.getDescription() : existing.getDescription();
        String location = body.getLocation() != null ? body.getLocation() : existing.getLocation();
        Boolean isBar = body.isBar();
        String cbu = body.getCbu() != null ? body.getCbu() : existing.getCbu();

        Club updated = clubService.update(
                id,
                name,
                location,
                existing.getRating(),
                existing.getQuantityRatings(),
                price,
                description,
                existing.getContactInfo() != null
                        ? existing.getContactInfo().getId()
                        : null,
                genre,
                zone,
                isBar,
                cbu
        );

        return Response.ok(toDtoWithLinks(updated)).build();
    }

    @GET
    @Path("/{clubId}/layout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLayout(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        if (clubService.getClubById(id).isEmpty())
            return notFound("Club no encontrado");

        return clubLayoutService.getLayoutByClub(id)
                .map(layout -> {
                    String imageUrl = uriInfo.getBaseUriBuilder()
                            .path("clubs")
                            .path(id.toString())
                            .path("layout")
                            .path("image")
                            .build()
                            .toString();

                    return Response.ok(new LayoutDto(
                            id.toString(),
                            imageUrl,
                            buildLayoutLinks(id)
                    )).build();
                })
                .orElse(notFound("Layout no encontrado"));
    }

    @GET
    @Path("/{clubId}/limits")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLimits(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        return clubService.getLimits(id)
                .map(limits -> {
                    limits.setLinks(buildLimitsLinks(id));
                    return Response.ok(limits).build();
                })
                .orElse(notFound("Club no encontrado"));
    }

    @GET
    @Path("/{clubId}/cover")
    public Response getCover(@PathParam("clubId") String clubId, @Context Request request) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        if (clubService.getClubById(id).isEmpty())
            return notFound("Club no encontrado");

        return photoService.getCoverPhoto(id)
                .map(photo -> buildImageResponse(photo, request))
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{clubId}/cover")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response putCover(@PathParam("clubId") String clubId,
                             @Context SecurityContext securityContext) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        Optional<Club> clubOpt = clubService.getClubById(id);
        if (clubOpt.isEmpty()) return notFound("Club no encontrado");

        User user = resolveAuthenticatedUser(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        clubService.assertAdmin(id, user.getId());

        try {
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                return badRequest("Missing multipart part 'file'");
            }

            String contentType = filePart.getContentType();
            validateImageContentType(contentType);

            byte[] data = filePart.getInputStream().readAllBytes();
            photoService.uploadCover(
                    id,
                    user.getId(),
                    extractFilename(filePart, "club-cover"),
                    contentType,
                    data
            );

            String coverUrl = uriInfo.getBaseUriBuilder()
                    .path("clubs")
                    .path(id.toString())
                    .path("cover")
                    .build()
                    .toString();

            Response.ResponseBuilder response = photoService.getCoverPhoto(id)
                    .map(photo -> Response.ok(photoDtoAssembler.toDto(uriInfo, photo)))
                    .orElseGet(Response::noContent);

            return response
                    .location(URI.create(coverUrl))
                    .header("X-Cover-Url", coverUrl)
                    .build();
        } catch (IOException | ServletException e) {
            return badRequest("Error procesando archivo de portada");
        }
    }

    @GET
    @Path("/{clubId}/photos")
    public Response getClubPhotos(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        try {
            if (clubService.getClubById(id).isEmpty()) {
                return notFound("Club no encontrado");
            }

            List<Photo> rawPhotos = photoService.getClubPhotos(id, 50, 0);
            List<PhotoDto> photos = new ArrayList<>();
            if (rawPhotos == null || rawPhotos.isEmpty()) {
                return Response.ok(new GenericEntity<List<PhotoDto>>(photos) {}).build();
            }

            for (Photo photo : rawPhotos) {
                if (photo == null || photo.getPhotoId() == null) {
                    continue;
                }
                photos.add(photoDtoAssembler.toDto(uriInfo, photo));
            }

            return Response.ok(new GenericEntity<List<PhotoDto>>(photos) {}).build();
        } catch (RuntimeException ex) {
            return Response.ok(new GenericEntity<List<PhotoDto>>(new ArrayList<PhotoDto>()) {}).build();
        }
    }

    @POST
    @Path("/{clubId}/photos")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response addClubPhoto(@PathParam("clubId") String clubId,
                                 @Context SecurityContext securityContext) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        Optional<Club> clubOpt = clubService.getClubById(id);
        if (clubOpt.isEmpty()) return notFound("Club no encontrado");

        User user = resolveAuthenticatedUser(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        clubService.assertAdmin(id, user.getId());

        try {
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                return badRequest("Missing multipart part 'file'");
            }

            String contentType = filePart.getContentType();
            validateImageContentType(contentType);

            byte[] data = filePart.getInputStream().readAllBytes();
            UUID photoId = photoService.uploadCarousel(
                    id,
                    user.getId(),
                    extractFilename(filePart, "club-photo"),
                    contentType,
                    data
            );

            URI location = uriInfo.getBaseUriBuilder()
                    .path("photos")
                    .path(photoId.toString())
                    .build();

            PhotoDto dto = photoService.findById(photoId)
                    .map(photo -> photoDtoAssembler.toDto(uriInfo, photo))
                    .orElseGet(() -> {
                        PhotoDto fallback = new PhotoDto();
                        fallback.setPhotoId(photoId);
                        fallback.setClubId(id);
                        fallback.setContentType(contentType);
                        fallback.setLinks(List.of(
                                LinkBuilder.link("self", location, "GET"),
                                LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                                        .path("clubs")
                                        .path(id.toString())
                                        .build(), "GET")
                        ));
                        return fallback;
                    });

            return Response.created(location)
                    .entity(dto)
                    .build();
        } catch (IllegalStateException ex) {
            return badRequest(ex.getMessage());
        } catch (IOException | ServletException e) {
            return badRequest("Error procesando archivo de galeria");
        }
    }

    @DELETE
    @Path("/{clubId}/photos/{photoId}")
    public Response deleteClubPhoto(@PathParam("clubId") String clubId,
                                    @PathParam("photoId") String photoId,
                                    @Context SecurityContext securityContext) {
        UUID clubUuid = parseUuidOr400(clubId);
        if (clubUuid == null) return badRequest("clubId inválido");

        UUID photoUuid = parseUuidOr400(photoId);
        if (photoUuid == null) return badRequest("photoId inválido");

        Optional<Club> clubOpt = clubService.getClubById(clubUuid);
        if (clubOpt.isEmpty()) return notFound("Club no encontrado");

        User user = resolveAuthenticatedUser(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        clubService.assertAdmin(clubUuid, user.getId());

        Optional<Photo> photoOpt = photoService.getWithId(photoUuid);
        if (photoOpt.isEmpty()) return notFound("Foto no encontrada");

        Photo photo = photoOpt.get();
        if (!clubUuid.equals(photo.getClubId())) {
            return notFound("Foto no encontrada para este club");
        }
        if (photo.getPhotoType() != PhotoType.CAROUSEL) {
            return badRequest("Solo se pueden eliminar fotos de galeria");
        }

        boolean deleted = photoService.delete(photoUuid);
        if (!deleted) {
            return notFound("Foto no encontrada");
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/{clubId}/review-photos")
    public Response getClubReviewPhotos(@PathParam("clubId") String clubId) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        try {
            if (clubService.getClubById(id).isEmpty()) {
                return notFound("Club no encontrado");
            }

            List<Photo> rawPhotos = photoService.getReviewPhotosByClub(id, 50, 0);
            List<PhotoDto> photos = new ArrayList<>();
            if (rawPhotos == null || rawPhotos.isEmpty()) {
                return Response.ok(new GenericEntity<List<PhotoDto>>(photos) {}).build();
            }

            for (Photo photo : rawPhotos) {
                if (photo == null || photo.getPhotoId() == null) {
                    continue;
                }
                photos.add(photoDtoAssembler.toDto(uriInfo, photo));
            }

            return Response.ok(new GenericEntity<List<PhotoDto>>(photos) {}).build();
        } catch (RuntimeException ex) {
            return Response.ok(new GenericEntity<List<PhotoDto>>(new ArrayList<PhotoDto>()) {}).build();
        }
    }

    @GET
    @Path("/{clubId}/layout/image")
    public Response getLayoutImage(@PathParam("clubId") String clubId, @Context Request request) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        return clubLayoutService.getLayoutByClub(id)
                .map(layout -> {
                    // ETag basado en layoutId + createdAt (cambia al reemplazar el layout)
                    String etagValue = layout.getLayoutId().toString()
                            + (layout.getCreatedAt() != null ? layout.getCreatedAt().toEpochSecond() : "0");
                    EntityTag etag = new EntityTag(Integer.toHexString(etagValue.hashCode()));

                    Response.ResponseBuilder preconditions = request.evaluatePreconditions(etag);

                    // Cache condicional: no-cache + ETag (recurso modificable)
                    CacheControl cacheControl = new CacheControl();
                    cacheControl.setNoCache(true);
                    cacheControl.setPrivate(false);

                    if (preconditions != null) {
                        return preconditions
                                .cacheControl(cacheControl)
                                .tag(etag)
                                .build();
                    }

                    return Response.ok(layout.getData(), layout.getContentType())
                            .cacheControl(cacheControl)
                            .tag(etag)
                            .header("Content-Length", layout.getSizeBytes())
                            .build();
                })
                .orElse(Response.noContent().build());
    }

    @PUT
    @Path("/{clubId}/layout")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putLayout(@PathParam("clubId") String clubId,
                              @Context SecurityContext securityContext) {
        UUID id = parseUuidOr400(clubId);
        if (id == null) return badRequest("clubId inválido");

        Optional<Club> clubOpt = clubService.getClubById(id);
        if (clubOpt.isEmpty()) return notFound("Club no encontrado");

        User user = resolveAuthenticatedUser(securityContext);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        clubService.assertAdmin(id, user.getId());

        try {
            Part filePart = request.getPart("file");
            if (filePart == null || filePart.getSize() == 0) {
                return badRequest("Missing multipart part 'file'");
            }

            String contentType = filePart.getContentType();
            validateImageContentType(contentType);

            byte[] data = filePart.getInputStream().readAllBytes();
            clubLayoutService.uploadLayout(
                    id,
                    user.getEmail(),
                    extractFilename(filePart, "club-layout"),
                    contentType,
                    data
            );

            String imageUrl = uriInfo.getBaseUriBuilder()
                    .path("clubs")
                    .path(id.toString())
                    .path("layout")
                    .path("image")
                    .build()
                    .toString();

            return Response.ok(new LayoutDto(
                            id.toString(),
                            imageUrl,
                            buildLayoutLinks(id)
                    ))
                    .location(URI.create(imageUrl))
                    .header("X-Layout-Url", imageUrl)
                    .build();
        } catch (IOException | ServletException e) {
            return badRequest("Error procesando archivo de layout");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private List<LinkDto> buildLayoutLinks(UUID clubId) {
        return List.of(
                LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(clubId.toString())
                        .path("layout")
                        .build(), "GET"),
                LinkBuilder.link("image", uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(clubId.toString())
                        .path("layout")
                        .path("image")
                        .build(), "GET"),
                LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(clubId.toString())
                        .build(), "GET")
        );
    }

    private List<LinkDto> buildLimitsLinks(UUID clubId) {
        return List.of(
                LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(clubId.toString())
                        .path("limits")
                        .build(), "GET"),
                LinkBuilder.link("club", uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(clubId.toString())
                        .build(), "GET"),
                LinkBuilder.link("tables", uriInfo.getBaseUriBuilder()
                        .path("clubs")
                        .path(clubId.toString())
                        .path("tables")
                        .build(), "GET")
        );
    }

    private ClubDto toDtoWithLinks(Club club) {

        ClubDto dto = new ClubDto();

        dto.setClubId(club.getClubId());
        dto.setClubName(club.getClubName());
        dto.setDescription(club.getDescription());
        dto.setLocation(club.getLocation());
        dto.setZone(club.getZone().name());
        dto.setMusicGenre(club.getMusicGenre().toString());
        dto.setPriceRange(club.getPriceRange().toString());
        dto.setBar(club.getIsBar());
        dto.setCbu(club.getCbu());
        final int quantityRatings = club.getQuantityRatings() != null
                ? club.getQuantityRatings()
                : 0;
        dto.setRating(quantityRatings > 0 && club.getRating() != null
                ? club.getRating().floatValue()
                : 0f);
        dto.setQuantityRatings(quantityRatings);
        dto.setCreatedAt(club.getCreatedAt());


        UUID clubId = club.getClubId();
        List<LinkDto> links = new ArrayList<>();
        links.add(LinkBuilder.link("self", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .build(), "GET"));
        links.add(LinkBuilder.link("events", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("events")
                .build(), "GET"));
        links.add(LinkBuilder.link("bookings", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("bookings")
                .build(), "GET"));
        links.add(LinkBuilder.link("reviews", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("reviews")
                .build(), "GET"));
        links.add(LinkBuilder.link("photos", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("photos")
                .build(), "GET"));
        links.add(LinkBuilder.link("review-photos", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("review-photos")
                .build(), "GET"));
        links.add(LinkBuilder.link("cover", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("cover")
                .build(), "GET"));
        links.add(LinkBuilder.link("layout", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("layout")
                .build(), "GET"));
        links.add(LinkBuilder.link("menu", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("menu")
                .build(), "GET"));
        links.add(LinkBuilder.link("tables", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("tables")
                .build(), "GET"));
        if (club.getContactInfo() != null) {
            links.add(LinkBuilder.link("owner", uriInfo.getBaseUriBuilder()
                    .path("users")
                    .path(club.getContactInfo().getId().toString())
                    .build(), "GET"));
        }
        links.add(LinkBuilder.link("limits", uriInfo.getBaseUriBuilder()
                .path("clubs")
                .path(clubId.toString())
                .path("limits")
                .build(), "GET"));

        dto.setLinks(links);

        return dto;
    }

    private void addPaginationLinks(Response.ResponseBuilder responseBuilder,
                                    int page,
                                    int size,
                                    int totalPages,
                                    int firstPage) {
        List<String> links = new ArrayList<>();
        if (page > firstPage) {
            links.add(pageLink(firstPage, size, "first"));
            links.add(pageLink(page - 1, size, "prev"));
        }
        if (page < totalPages) {
            links.add(pageLink(page + 1, size, "next"));
            links.add(pageLink(totalPages, size, "last"));
        }
        if (!links.isEmpty()) {
            responseBuilder.header("Link", String.join(", ", links));
        }
    }

    private String pageLink(int page, int size, String rel) {
        return "<" + uriInfo.getRequestUriBuilder()
                .replaceQueryParam("page", page)
                .replaceQueryParam("size", size)
                .build() + ">; rel=\"" + rel + "\"";
    }


    private static UUID parseUuidOr400(final String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Response badRequest(final String msg) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDto(msg))
                .build();
    }

    private static Response notFound(final String msg) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorDto(msg))
                .build();
    }

    private User resolveAuthenticatedUser(SecurityContext securityContext) {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return resolveUserFromSpringSecurityContext();
        }

        String principal = securityContext.getUserPrincipal().getName();
        User user = resolveUserFromPrincipal(principal);
        if (user != null) {
            return user;
        }

        return resolveUserFromSpringSecurityContext();
    }

    private User resolveUserFromSpringSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof PawUserDetails) {
            return ((PawUserDetails) principal).getUser();
        }

        return resolveUserFromPrincipal(auth.getName());
    }

    private User resolveUserFromPrincipal(String principal) {
        if (principal == null || principal.trim().isEmpty()) {
            return null;
        }

        User user = userService.findByEmail(principal);
        if (user != null) {
            return user;
        }

        user = userService.findByUserName(principal);
        if (user != null) {
            return user;
        }

        try {
            return userService.findById(UUID.fromString(principal)).orElse(null);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void validateImageContentType(String contentType) {
        if (contentType == null) {
            throw new BadRequestException("Missing content type");
        }

        switch (contentType) {
            case "image/jpeg":
            case "image/jpg":
            case "image/png":
            case "image/webp":
            case "image/gif":
            case "image/avif":
                return;
            default:
                throw new BadRequestException("Unsupported image content type");
        }
    }

    private String extractFilename(Part part, String fallback) {
        String filename = part.getSubmittedFileName();
        if (filename == null || filename.trim().isEmpty()) {
            return fallback;
        }
        return filename;
    }

    /**
     * Respuesta para imágenes modificables (cover del club).
     * Usa Cache-Control: no-cache + ETag basado en photoId + createdAt.
     * El cliente puede cachear localmente pero debe revalidar antes de usar la copia.
     */
    private Response buildImageResponse(Photo photo, Request request) {
        String etagValue = photo.getPhotoId().toString()
                + (photo.getCreatedAt() != null ? photo.getCreatedAt().toEpochSecond() : "0");
        EntityTag etag = new EntityTag(Integer.toHexString(etagValue.hashCode()));

        Response.ResponseBuilder preconditions = request.evaluatePreconditions(etag);

        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        cacheControl.setPrivate(false);

        if (preconditions != null) {
            return preconditions
                    .cacheControl(cacheControl)
                    .tag(etag)
                    .build();
        }

        return Response.ok(photo.getData(), photo.getContentType())
                .cacheControl(cacheControl)
                .tag(etag)
                .header("Content-Length", photo.getSizeBytes())
                .build();
    }
}
