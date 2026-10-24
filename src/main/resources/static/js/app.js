// Comportamentos comuns: seleção em listas, linhas dinâmicas, edição inline e confirmações.
(function () {
    "use strict";

    // Mensagens somem sozinhas após alguns segundos.
    document.querySelectorAll(".o-flash .alert-success").forEach(function (alert) {
        setTimeout(function () { bootstrap.Alert.getOrCreateInstance(alert).close(); }, 5000);
    });

    // Seleção múltipla: [data-select-all] marca tudo; botões [data-needs-selection] só ficam ativos com seleção.
    document.querySelectorAll("[data-selectable]").forEach(function (table) {
        var all = table.querySelector("[data-select-all]");
        var boxes = Array.from(table.querySelectorAll("input[name=ids][type=checkbox]"));
        var counter = document.querySelector(".o-selection");
        var needs = document.querySelectorAll("[data-needs-selection]");

        function refresh() {
            var selected = boxes.filter(function (b) { return b.checked; });
            boxes.forEach(function (b) { b.closest("tr").classList.toggle("selected", b.checked); });
            if (all) {
                all.checked = selected.length > 0 && selected.length === boxes.length;
                all.indeterminate = selected.length > 0 && selected.length < boxes.length;
            }
            if (counter) {
                counter.classList.toggle("show", selected.length > 0);
                counter.querySelector("[data-count]").textContent = selected.length;
            }
            needs.forEach(function (el) { el.classList.toggle("d-none", selected.length === 0); });
        }

        if (all) {
            all.addEventListener("change", function () {
                boxes.forEach(function (b) { b.checked = all.checked; });
                refresh();
            });
        }
        boxes.forEach(function (b) { b.addEventListener("change", refresh); });
        if (counter) {
            counter.querySelector("[data-clear]").addEventListener("click", function () {
                boxes.forEach(function (b) { b.checked = false; });
                refresh();
            });
        }
        refresh();
    });

    // Linhas clicáveis abrem o registro (exceto ao clicar em controles).
    document.querySelectorAll("tr[data-href]").forEach(function (row) {
        row.classList.add("clickable");
        row.addEventListener("click", function (e) {
            if (e.target.closest("a, button, input, select, label, form")) { return; }
            window.location = row.dataset.href;
        });
    });

    // Edição inline: salva ao alterar o campo.
    document.querySelectorAll("input[data-autosubmit]").forEach(function (input) {
        input.addEventListener("change", function () { input.form.requestSubmit(); });
    });

    // Confirmação para ações destrutivas.
    document.addEventListener("submit", function (e) {
        var submitter = e.submitter;
        var message = (submitter && submitter.dataset.confirm) || e.target.dataset.confirm;
        if (message && !window.confirm(message)) { e.preventDefault(); }
    });

    // Linhas de produtos (operações e pedidos): adicionar/remover reindexando os nomes lines[i].campo.
    document.querySelectorAll("[data-lines]").forEach(function (container) {
        var body = container.querySelector("tbody");
        var template = container.querySelector("template");

        function reindex() {
            body.querySelectorAll("tr").forEach(function (row, i) {
                row.querySelectorAll("[name^='lines[']").forEach(function (field) {
                    field.name = field.name.replace(/lines\[\d+\]/, "lines[" + i + "]");
                });
            });
            updateTotal();
        }

        function updateTotal() {
            var totalEl = container.querySelector("[data-total]");
            if (!totalEl) { return; }
            var total = 0;
            body.querySelectorAll("tr").forEach(function (row) {
                var qty = parseFloat((row.querySelector("[name$='.quantity']") || {}).value) || 0;
                var price = parseFloat((row.querySelector("[name$='.unitPrice']") || {}).value) || 0;
                var subtotal = qty * price;
                var cell = row.querySelector("[data-subtotal]");
                if (cell) { cell.textContent = subtotal.toLocaleString("pt-BR", { style: "currency", currency: "BRL" }); }
                total += subtotal;
            });
            totalEl.textContent = total.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
        }

        container.addEventListener("click", function (e) {
            if (e.target.closest("[data-add-line]")) {
                e.preventDefault();
                body.appendChild(template.content.cloneNode(true));
                reindex();
            }
            var remove = e.target.closest("[data-remove-line]");
            if (remove) {
                e.preventDefault();
                remove.closest("tr").remove();
                if (body.querySelectorAll("tr").length === 0) { body.appendChild(template.content.cloneNode(true)); }
                reindex();
            }
        });

        // Ao escolher um produto em compras, sugere o custo como preço.
        container.addEventListener("change", function (e) {
            if (e.target.matches("select[name$='.productId']")) {
                var price = e.target.closest("tr").querySelector("[name$='.unitPrice']");
                var option = e.target.selectedOptions[0];
                if (price && option && option.dataset.cost && !price.value) { price.value = option.dataset.cost; }
            }
            updateTotal();
        });
        container.addEventListener("input", updateTotal);
        reindex();
    });

    // Tooltips do Bootstrap.
    document.querySelectorAll("[data-bs-toggle='tooltip']").forEach(function (el) {
        new bootstrap.Tooltip(el);
    });
})();
