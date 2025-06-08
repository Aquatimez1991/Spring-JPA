package com.aluracursos.screenmatch.principal;

import com.aluracursos.screenmatch.model.*;
import com.aluracursos.screenmatch.repository.SerieRepository;
import com.aluracursos.screenmatch.service.ConsumoAPI;
import com.aluracursos.screenmatch.service.ConvierteDatos;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=fe334a9a";
    private ConvierteDatos conversor = new ConvierteDatos();
    private List<DatosSerie> datosSeries = new ArrayList<>();

    private SerieRepository repositorio;

    private List<Serie> series;

    public Principal(SerieRepository repository) {
        this.repositorio = repository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    1 - Buscar series 
                    2 - Buscar episodios
                    3 - Mostrar series buscadas
                    4 - Buscar series por titulo
                    5 - Top 5 mejores series
                    6 - Buscar series por Categoría
                    7 - Buscar series por Evaluación
                    8 - Buscar series por número de episodios
                    9 - Filtrar series
                    10 - Filtrar series por temporada y evaluación (SQL)
                                  
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    mostrarSeriesBuscadas();
                    break;

                case 4:
                    buscarSeriesPorTitulo();
                    break;

                case 5:
                    buscarTop5Series();
                    break;
                case 6:
                    buscarSeriesPorCategoria();
                    break;

                case 7:
                    buscarSeriesPorEvaluacion();
                    break;

                case 8:
                    buscarSeriesPorNumeroDeEpisodios();
                    break;

                case 9:
                    filtrarSeriesPorTemporadaYEvaluacion();
                    break;

                case 10:
                    filtrarSeriesPorTemporadaYEvaluacionSQL();
                    break;

                case 0:
                    System.out.println("Cerrando la aplicación...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }

    }




    private DatosSerie getDatosSerie() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        var json = consumoApi.obtenerDatos(URL_BASE + nombreSerie.replace(" ", "+") + API_KEY);
        System.out.println(json);
        DatosSerie datos = conversor.obtenerDatos(json, DatosSerie.class);
        return datos;
    }
    private void buscarEpisodioPorSerie() {
        mostrarSeriesBuscadas();
        System.out.println("Escribe el nombre de la serie que deseas buscar sus episodios");
        var nombreSerie = teclado.nextLine();

        Optional<Serie> serie = series.stream()
                .filter(s->s.getTitulo().toLowerCase().contains(nombreSerie.toLowerCase()))
                .findFirst();

        if(serie.isPresent()){
            var serieEncontrada = serie.get();
            List<DatosTemporadas> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumoApi.obtenerDatos(URL_BASE + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DatosTemporadas datosTemporada = conversor.obtenerDatos(json, DatosTemporadas.class);
                temporadas.add(datosTemporada);
            }
            temporadas.forEach(System.out::println);
            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d-> d.episodios().stream()
                            .map(e->new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());

            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);
        }


    }
    private void buscarSerieWeb() {
        DatosSerie datos = getDatosSerie();
        Serie serie = new Serie(datos);
        repositorio.save(serie);
     //   datosSeries.add(datos);
        System.out.println(datos);
    }

    private void mostrarSeriesBuscadas() {
        series = repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriesPorTitulo() {
        System.out.println("Escribe el nombre de la serie que deseas buscar");
        var nombreSerie = teclado.nextLine();
        Optional<Serie> serieBuscada = repositorio.findByTituloContainingIgnoreCase(nombreSerie);
        if (serieBuscada.isPresent()) {
            System.out.println("Serie encontrada es: " + serieBuscada.get());
        } else {
            System.out.println("No se encontró ninguna serie con ese título.");
        }
    }

    private void buscarTop5Series() {
        List<Serie> topSeries = repositorio.findTop5ByOrderByEvaluacionDesc();
        topSeries.forEach(s->
                System.out.println("Serie: " + s.getTitulo() + ", Evaluación: " + s.getEvaluacion()));
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Escriba el genero/categoria de la serie que desea buscar");
        var genero = teclado.nextLine();
        var cateroria = Categoria.fromEspanol(genero);
        List<Serie> seriesPorCategoria = repositorio.findByGenero(cateroria);
        System.out.println("Las series de la categoria " + genero);
        seriesPorCategoria.forEach(System.out::println);

    }

    private void buscarSeriesPorEvaluacion() {
        System.out.println("Escribe la evaluación de la serie que deseas buscar");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> seriesPorEvaluacion = repositorio.findByEvaluacion(evaluacion);
        if (seriesPorEvaluacion.isEmpty()) {
            System.out.println("No se encontraron series con esa evaluación.");
        } else {
            System.out.println("Las series con una evaluación de " + evaluacion + " son:");
            seriesPorEvaluacion.forEach(System.out::println);
        }
    }

    private void buscarSeriesPorNumeroDeEpisodios() {
        System.out.println("Escribe el número de episodios que deseas buscar:");
        var numeroEpisodios = teclado.nextInt();
        teclado.nextLine();

        if (series == null || series.isEmpty()) {
            System.out.println("No hay series cargadas en el sistema.");
            return;
        }

        List<Serie> seriesPorEpisodios = repositorio.findByEpisodios_NumeroEpisodio(numeroEpisodios);

        if (seriesPorEpisodios.isEmpty()) {
            System.out.println("No se encontraron series con ese número de episodios.");
        } else {
            System.out.println("Las series con " + numeroEpisodios + " episodios son:");
            seriesPorEpisodios.forEach(System.out::println);
        }
    }

    private void filtrarSeriesPorTemporadaYEvaluacion() {
        System.out.println("¿Cuántas temporadas debe tener una serie para ser filtrada? ");
        var totalTemporadas = teclado.nextInt();
        teclado.nextLine();
        System.out.println("¿Cual debe ser el valor base de la evaluación? ");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> filtroSeries = repositorio.findByTotalTemporadasLessThanEqualAndEvaluacionGreaterThanEqual(totalTemporadas, evaluacion);
        System.out.println("*** Series filtradas ***");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + "  - evaluacion: " + s.getEvaluacion()));
    }

    private void filtrarSeriesPorTemporadaYEvaluacionSQL() {
        System.out.println("¿Cuántas temporadas debe tener una serie para ser filtrada? ");
        var totalTemporadas = teclado.nextInt();
        teclado.nextLine();
        System.out.println("¿Cual debe ser el valor base de la evaluación? ");
        var evaluacion = teclado.nextDouble();
        teclado.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaYEvaluacion(totalTemporadas,evaluacion);
        System.out.println("*** Series filtradas ***");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + "  - evaluacion: " + s.getEvaluacion()));
    }
    }




