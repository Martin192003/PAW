package ar.edu.itba.paw.webapp.controller;


import ar.edu.itba.paw.model.*;
import ar.edu.itba.paw.model.exceptions.*;
import ar.edu.itba.paw.service.*;
import ar.edu.itba.paw.webapp.form.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/clubs/tables")
public class TablesController {

     private final TableService tableService;

     @Autowired
     public TablesController(TableService tableService) {
         this.tableService = tableService;
     }

    @PostMapping("/{tableId}/edit")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#form.clubId)")
    public String editTable(
            @PathVariable UUID tableId,
            @Valid @ModelAttribute("editTableForm") EditTableForm form,
            BindingResult errors,
            RedirectAttributes ra
    ) {
        if (errors.hasErrors()) {
            ra.addAttribute("err", "validation");
            return "redirect:/clubs/handle?clubId=" + form.getClubId();
        }

        try {
            tableService.update(
                    tableId,
                    form.getClubId(),
                    form.getTableNumber(),
                    form.getCapacity(),
                    TableState.DISPONIBLE,
                    form.getPrice(),
                    form.getDiscount()
            );
        } catch (DuplicateEntityException | DataIntegrityViolationException e) {
            ra.addAttribute("err", "dup");
        } catch (NotFoundException e) {
            ra.addAttribute("err", "club_not_found");
        } catch (InvalidTableNumberException e) {
            ra.addAttribute("err", "table_number");
        } catch (InvalidCapacityException e) {
            ra.addAttribute("err", "capacity");
        } catch (InvalidPriceException e) {
            ra.addAttribute("err", "price");
        } catch (ValidationException e) {
            ra.addAttribute("err", "discount");
        } catch (IllegalStateException e) {
            ra.addAttribute("err", "max_tables");
        }
        return "redirect:/clubs/handle?clubId=" + form.getClubId();
    }


    @PostMapping("/{tableId}/delete")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#clubId)")
    public String deleteTable(@PathVariable UUID tableId, @RequestParam("clubId") UUID clubId) {
        tableService.delete(tableId);
        return "redirect:/clubs/handle?clubId=" + clubId;
    }

    @PostMapping("/addTable")
    @PreAuthorize("@securityServiceImpl.isClubAdmin(#tableForm.clubId)")
    public String addTable(
            @Valid @ModelAttribute("tableForm") AddTableForm tableForm,
            BindingResult errors,
            RedirectAttributes ra
    ) {
        if (tableForm.getDiscount() == null) {
            tableForm.setDiscount(BigDecimal.ZERO);
        }

        if (errors.hasErrors()) {
            ra.addAttribute("err", "validation");
            return "redirect:/clubs/handle?clubId=" + tableForm.getClubId();
        }

        try {
            tableService.create(
                    tableForm.getTableNumber(),
                    tableForm.getCapacity(),
                    tableForm.getClubId(),
                    TableState.DISPONIBLE,
                    tableForm.getPrice(),
                    tableForm.getDiscount()
            );
        } catch (DuplicateEntityException | DataIntegrityViolationException e) {
            ra.addAttribute("err", "dup");
        } catch (NotFoundException e) {
            ra.addAttribute("err", "club_not_found");
        } catch (InvalidTableNumberException e) {
            ra.addAttribute("err", "table_number");
        } catch (InvalidCapacityException e) {
            ra.addAttribute("err", "capacity");
        } catch (InvalidPriceException e) {
            ra.addAttribute("err", "price");
        } catch (ValidationException e) {
            ra.addAttribute("err", "discount");
        } catch (IllegalStateException e) {
            ra.addAttribute("err", "max_tables");
        }
        return "redirect:/clubs/handle?clubId=" + tableForm.getClubId();
    }

}
