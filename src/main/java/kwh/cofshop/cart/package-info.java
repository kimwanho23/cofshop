@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.CLOSED,
        allowedDependencies = {
                "member::domain",
                "member::api",
                "member::event",
                "item::domain",
                "item::api",
                "global::annotation",
                "global::exception",
                "global::errorcodes"
        }
)
package kwh.cofshop.cart;
