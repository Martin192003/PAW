package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Product;
import ar.edu.itba.paw.persistence.ClubDao;
import ar.edu.itba.paw.persistence.ProductsDao;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class MenuServiceImpl implements MenuService {

    private final ProductsDao productsDao;
    private final ClubDao clubDao;

    public MenuServiceImpl(ProductsDao productsDao, ClubDao clubDao) {
        this.productsDao = productsDao;
        this.clubDao = clubDao;
    }

    @Override
    public List<Product> getMenuByClub(UUID clubId) {
        return productsDao.findByClub(clubDao.findById(clubId).orElseThrow(IllegalArgumentException::new));
    }

}
