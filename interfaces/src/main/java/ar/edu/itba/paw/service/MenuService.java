package ar.edu.itba.paw.service;

import ar.edu.itba.paw.model.Product;
import java.util.List;
import java.util.UUID;

public interface MenuService {
    List<Product> getMenuByClub(UUID clubId);
}