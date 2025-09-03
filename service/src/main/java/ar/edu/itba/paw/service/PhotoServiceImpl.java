
package ar.edu.itba.paw.service;

import ar.edu.itba.paw.persistence.PhotoDao;
import ar.edu.itba.paw.service.PhotoService;
import ar.edu.itba.paw.model.Photo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PhotoServiceImpl implements PhotoService {

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB (ajustá si querés)
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final PhotoDao photoDao;

    public PhotoServiceImpl(PhotoDao photoDao) {
        this.photoDao = photoDao;
    }

    @Override
    @Transactional
    public UUID upload(UUID clubId,
                       UUID postedBy,
                       String filename,
                       String contentType,
                       byte[] data) {

        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("La imagen está vacía");
        }
        if (data.length > MAX_BYTES) {
            throw new IllegalArgumentException("La imagen excede el tamaño máximo permitido");
        }
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de imagen no permitido");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename inválido");
        }

        return photoDao.create(clubId, postedBy, filename, contentType, data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Photo> listByClub(UUID clubId, int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, pageSize);
        int offset = safePage * safeSize;
        return photoDao.findByClub(clubId, safeSize, offset);
    }

    @Override
    public Optional<Photo> findById(UUID photoId) {
        return photoDao.findById(photoId);
    }

    @Override
    public Optional<Photo> getWithId(UUID photoId) {
        return photoDao.findByIdWithData(photoId);
    }

    @Override
    public UUID save(Photo p) {
        return photoDao.save(p);
    }

    @Override
    @Transactional
    public boolean delete(UUID photoId) {
        return photoDao.delete(photoId);
    }
}