package com.example.marketplace.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.marketplace.model.ItemCarrinho;
import com.example.marketplace.model.Produto;
import com.example.marketplace.model.ResumoCarrinho;
import com.example.marketplace.model.SelecaoCarrinho;
import com.example.marketplace.repository.ProdutoRepository;

@Service
public class ServicoCarrinho {

    private final ProdutoRepository repositorioProdutos;

    public ServicoCarrinho(ProdutoRepository repositorioProdutos) {
        this.repositorioProdutos = repositorioProdutos;
    }

    public ResumoCarrinho construirResumo(List<SelecaoCarrinho> selecoes) {

        List<ItemCarrinho> itens = new ArrayList<>();

        // =========================
        // Monta os itens do carrinho
        // =========================
        for (SelecaoCarrinho selecao : selecoes) {
            Produto produto = repositorioProdutos.buscarPorId(selecao.getProdutoId())
                    .orElseThrow(
                            () -> new IllegalArgumentException("Produto não encontrado: " + selecao.getProdutoId()));

            itens.add(new ItemCarrinho(produto, selecao.getQuantidade()));
        }

        // =========================
        // Calcula subtotal
        // =========================
        BigDecimal subtotal = itens.stream()
                .map(ItemCarrinho::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItens = itens.stream()
                .mapToInt(ItemCarrinho::getQuantidade)
                .sum();

        BigDecimal descontoQuantidade = BigDecimal.ZERO;
        if (totalItens == 2) {
            descontoQuantidade = BigDecimal.valueOf(5);
        } else if (totalItens == 3) {
            descontoQuantidade = BigDecimal.valueOf(7);
        } else if (totalItens >= 4) {
            descontoQuantidade = BigDecimal.valueOf(10);
        }

        BigDecimal descontoCategoria = BigDecimal.ZERO;
        for (ItemCarrinho item : itens) {
            BigDecimal descontoItemCategoria = BigDecimal.ZERO;
            
            switch (item.getProduto().getCategoria()) {
                case CAPINHA:
                    descontoItemCategoria = BigDecimal.valueOf(3);
                    break;
                case CARREGADOR:
                    descontoItemCategoria = BigDecimal.valueOf(5);
                    break;
                case FONE:
                    descontoItemCategoria = BigDecimal.valueOf(3);
                    break;
                case PELICULA:
                    descontoItemCategoria = BigDecimal.valueOf(2);
                    break;
                case SUPORTE:
                    descontoItemCategoria = BigDecimal.valueOf(2);
                    break;
            }
            
            descontoCategoria = descontoCategoria.add(
                    descontoItemCategoria.multiply(BigDecimal.valueOf(item.getQuantidade()))
            );
        }

        BigDecimal percentualDesconto = descontoQuantidade.add(descontoCategoria);
        
        if (percentualDesconto.compareTo(BigDecimal.valueOf(25)) > 0) {
            percentualDesconto = BigDecimal.valueOf(25);
        }

        BigDecimal valorDesconto = subtotal
                .multiply(percentualDesconto)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal total = subtotal.subtract(valorDesconto);

        return new ResumoCarrinho(itens, subtotal, percentualDesconto, valorDesconto, total);
    }
}
