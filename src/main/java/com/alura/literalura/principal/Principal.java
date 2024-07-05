package com.alura.literalura.principal;

import com.alura.literalura.model.*;
import com.alura.literalura.repository.LibrosRepository;
import com.alura.literalura.service.ConsumoAPI;
import com.alura.literalura.service.ConvierteDatos;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private static final String URL_BASE = "https://gutendex.com/books/";
    private ConsumoAPI consumoAPI = new ConsumoAPI();
    private ConvierteDatos conversor = new ConvierteDatos();
    private LibrosRepository repositorio;

    public Principal (LibrosRepository repository){
        this.repositorio=repository;
    }

    public void mostrarMenu(){
        var opcion = -1;
        while (opcion != 0){

            var menu = """
                    ----------------------------------------
                    Elija la opción que desea (solo numero) :
                    1- Buscar libro por título
                    2- Listar libros registrados
                    3- Listar autores registrados
                    4- Listar autores vivos en un determinado año
                    5- Listar libros por idioma
                    0- Salir
                    ----------------------------------------
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion){
                case 1:
                    buscarLibro();
                    break;
                case 2:
                    mostrarLibros();
                    break;
                case 3:
                    mostrarAutores();
                    break;
                case 4:
                    mostrarAutoresPorAnio();
                    break;
                case 5:
                    mostrarLibrosPorIdioma();
                    break;
                case 0:
                    System.out.println("Cerrando aplicación");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }


    private void mostrarLibrosPorIdioma() {
        System.out.println("""
                Escriba el idioma del libro:
                ES: Español
                EN: Ingles
                FR: Frances
                IT: Italiano
                PT: Portugues
                """);

        var idiomaSelecionado = teclado.nextLine();

        try {
            List<Libro> libroPorIdioma = repositorio.findByIdiomas(Idioma.valueOf(idiomaSelecionado.toUpperCase()));
            libroPorIdioma.forEach(n -> System.out.println(
                    "********* LIBRO *********" +
                            "\nTitulo: " + n.getTitulo() +
                            "\nIndioma: " + n.getIdiomas() +
                            "\nAutor: " + n.getAutor().stream().map(Autor::getNombre).collect(Collectors.joining()) +
                            "\nNumero de descargas: " + n.getNumeroDeDescargas() +
                            "\n"
            ));
        } catch (IllegalArgumentException e){
            System.out.println("Idioma no encontrado...\n");
        }

    }

    private void mostrarAutoresPorAnio() {
        System.out.println("Ingresa el año a consultar:");
        String anio = teclado.nextLine();

        List<Autor> autoresVivos = repositorio.mostrarAutoresVivos(anio);

        if (autoresVivos.isEmpty()){
            System.out.println("Sin autores vivos en el año indicado...\n");
            return;
        }

        Map<String, List<String>> autoresConLibros = autoresVivos.stream()
                .collect(Collectors.groupingBy(
                        Autor::getNombre,
                        Collectors.mapping(a -> a.getLibro().getTitulo(), Collectors.toList())
                ));

        autoresConLibros.forEach((nombre, libros) -> {
            Autor autor = autoresVivos.stream()
                    .filter(a -> a.getNombre().equals(nombre))
                    .findFirst().orElse(null);
            if (autor != null) {
                System.out.println("********* AUTOR *********");
                System.out.println("Nombre: " + nombre);
                System.out.println("Fecha de nacimiento: " + autor.getFechaDeNacimiento());
                System.out.println("Fecha de muerte: " + autor.getFechaDeMuerte());
                System.out.println("Libros: " + libros + "\n");
            }
        });
    }

    private void mostrarLibros() {
        List<Libro> mostrarListaLibros = repositorio.findAll();
        mostrarListaLibros.forEach(l -> System.out.println(
                "********* LIBRO *********" +
                        "\nTítulo: " + l.getTitulo()+
                        "\nIdioma: " + l.getIdiomas()+
                        "\nAutor: " + l.getAutor().stream().map(Autor::getNombre).collect(Collectors.joining()) +
                        "\nNúmero de descargas: " + l.getNumeroDeDescargas() +
                        "\n"
        ));
    }

    private void mostrarAutores(){
        List<Autor> mostarListaAutores = repositorio.mostrarAutores();

        Map<String, List<String>> autoresConLibros = mostarListaAutores.stream()
                .collect(Collectors.groupingBy(
                        Autor::getNombre,
                        Collectors.mapping(a -> a.getLibro().getTitulo(), Collectors.toList())
                ));

        autoresConLibros.forEach((nombre, libros) -> {
            Autor autor = mostarListaAutores.stream()
                    .filter(a -> a.getNombre().equals(nombre))
                    .findFirst().orElse(null);
            if (autor != null) {
                System.out.println("********* AUTOR *********");
                System.out.println("Nombre: " + nombre);
                System.out.println("Fecha de nacimiento: " + autor.getFechaDeNacimiento());
                System.out.println("Fecha de muerte: " + autor.getFechaDeMuerte());
                System.out.println("Libros: " + libros + "\n");
            }
        });
    }

    // Conversor en la API
    private DatosLibros buscarLibroAPI(){
        System.out.println("Ingresa el nombre del libro que desea buscar");
        var tituloLibro = teclado.nextLine();
        var json = consumoAPI.obtenerDatos(URL_BASE + "?search=" + tituloLibro.replace(" ", "+"));
        var datosBusqueda = conversor.obtenerDatos(json, Datos.class);

        Optional<DatosLibros> libroBuscado = datosBusqueda.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(tituloLibro.toUpperCase()))
                .findFirst();

        if (libroBuscado.isPresent()){
            System.out.println("Libro encontrado...");
            return libroBuscado.get();
        } else {
            System.out.println("Libro no encontrado.\n");
            return null;
        }
    }

    private void buscarLibro() {
        Optional<DatosLibros> datosOpcional = Optional.ofNullable(buscarLibroAPI());

        if (datosOpcional.isPresent()) {
            DatosLibros datos = datosOpcional.get();

            Libro libro = new Libro(datos);
            List<Autor> autores = new ArrayList<>();
            for (DatosAutor datosAutor : datos.autor()) {
                Autor autor = new Autor(datosAutor);
                autor.setLibro(libro);
                autores.add(autor);
            }
            libro.setAutor(autores);
            try {
                repositorio.save(libro);
                System.out.println(libro.getTitulo() + " almacenado con exito!");

                // Imprimir detalles del libro al estilo de mostrarLibrosConsola
                System.out.println("+++++++++ LIBRO +++++++++");
                System.out.println("Título: " + libro.getTitulo());
                System.out.println("Idioma: " + libro.getIdiomas());
                System.out.println("Autor: " + libro.getAutor().stream().map(Autor::getNombre).collect(Collectors.joining(", ")));
                System.out.println("Número de descargas: " + libro.getNumeroDeDescargas());
                System.out.println();
            } catch (DataIntegrityViolationException e) {
                System.out.println("Error: no se puede almacenar el libro más de una vez.\n");
            }
        }
    }


}
