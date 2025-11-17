package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.ClubLayout;
import ar.edu.itba.paw.model.User;
import ar.edu.itba.paw.persistence.ClubLayoutDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

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
    public void uploadLayout(UUID clubId, String userEmail, String filename, String contentType, byte[] data) {
        User uploader = userService.findByEmail(userEmail);
        clubService.assertAdmin(clubId, uploader.getId());

        layoutDao.deleteByClubId(clubId);

        ClubLayout layout = new ClubLayout();
        UUID newId = UUID.randomUUID();
        layout.setLayoutId(newId);
        layout.setClubId(clubId);
        layout.setUploadedBy(uploader.getId());
        layout.setFilename(filename);
        layout.setContentType(contentType != null ? contentType : "application/octet-stream");
        layout.setData(data);
        layout.setSizeBytes((long) data.length);
        layout.setUrl("/clubs/club/" + clubId + "/layout/" + newId + "/image");

        layoutDao.save(layout);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<ClubLayout> getLayoutByClub(UUID clubId) {
        return layoutDao.findByClubId(clubId);
    }

}

