package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.Photo;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.ClubLayoutDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClubLayoutServiceImpl implements ClubLayoutService {

    private final ClubLayoutDao layoutDao;
    private final UserService userService;
    private final ClubService clubService;

    @Autowired
    public ClubLayoutServiceImpl(ClubLayoutDao layoutDao, UserService userService, ClubService clubService) {
        this.layoutDao = layoutDao;
        this.userService = userService;
        this.clubService = clubService;
    }

    @Override
    @Transactional
    public UUID uploadLayout(UUID clubId, String userEmail, Photo file) {
            User uploader = userService.findByEmail(userEmail);
            clubService.assertAdmin(clubId, uploader.getId());

            // elimina el layout anterior si existe
            layoutDao.deleteByClubId(clubId);

            ClubLayout layout = new ClubLayout();

            // Generar id en la aplicación para poder construir la URL antes del persist
            UUID newId = UUID.randomUUID();
            layout.setLayoutId(newId);

            layout.setClubId(clubId);
            layout.setUploadedBy(uploader.getId());
            layout.setFilename(file.getFilename());
            layout.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");

            byte[] data = file.getData();
            layout.setData(data);
            layout.setSizeBytes((long) data.length);

            // Construir URL ahora que tenemos el id
            layout.setUrl("/clubs/club/" + clubId + "/layout/" + newId + "/image");

            layoutDao.save(layout);
            return layout.getLayoutId();
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<ClubLayout> getLayoutByClub(UUID clubId) {
        return layoutDao.findByClubId(clubId);
    }

    @Override
    @Transactional
    public boolean deleteLayout(UUID clubId) {
        return layoutDao.deleteByClubId(clubId);
    }
}

