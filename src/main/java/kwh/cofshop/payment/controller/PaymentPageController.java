package kwh.cofshop.payment.controller;

import kwh.cofshop.payment.properties.PortOneBrowserProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PaymentPageController {

    private final PortOneBrowserProperties portOneBrowserProperties;

    @GetMapping("/payments/sample")
    public String samplePaymentPage(Model model) {
        model.addAttribute("portOneStoreId", portOneBrowserProperties.getStoreId());
        model.addAttribute("portOneChannelKey", portOneBrowserProperties.getChannelKey());
        return "payInicis";
    }
}
