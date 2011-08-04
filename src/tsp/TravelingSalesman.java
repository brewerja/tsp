package tsp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TravelingSalesman {

	public static void main(String[] args) {
		int EVOLUTIONS = 40;
		ArrayList<Double> best = new ArrayList<Double>(EVOLUTIONS);
		for (int j = 0; j < EVOLUTIONS; ++j) {
			TravelingSalesman ts = new TravelingSalesman(args[0]);
			for (int i = 0; i < 100; ++i)
				ts.evolve();
			Collections.sort(ts.routes);
			Route topRoute = ts.routes.get(0);
//			topRoute.printRoute();
//			System.out.println(topRoute);
//			System.out.println("\n" + ts.calcRouteLength(topRoute.getRoute()));	
			best.add(topRoute.getRouteLength());
		}
		
		System.out.println("Evolutions Results:");
		Collections.sort(best);
		for (Double d: best)
			System.out.println(d);
		System.out.println("Best result:" + best.get(0));
	}

	private int numCities;
	private City[] baseCityArray;
	private ArrayList<Route> routes;
	private double[][] matrix;
	private final int POPULATION_SIZE = 20000;
	private final int EVOLVING_POPULATION_SIZE = 200;
	private Random generator = new Random();

	public TravelingSalesman(String path) {
		try {
			this.initialize(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initialize(String path) throws IOException {
		// Parse the file of city listings. (Name, x, y)
		FileReader file = new FileReader(path);
		BufferedReader bf = new BufferedReader(file);
		String line;
		ArrayList<City> list = new ArrayList<City>();
		while ((line = bf.readLine()) != null) {
			String[] params = line.split(",");
			String name = params[0].trim();
			double x = new Double(params[1].trim());
			double y = new Double(params[2].trim());
			City c = new City(name, list.size(), x, y);
			list.add(c);
		}

		// Store the number of cities to be traversed.
		numCities = list.size();
		//System.out.println(numCities + " cities read.");

		// Create the base route.
		baseCityArray = new City[numCities];
		for (int i = 0; i < numCities; ++i)
			baseCityArray[i] = list.get(i);

		// Create matrix to store distances between cities.
		createMatrix();

		// Create the initial population of randomized routes.
		ArrayList<Route> init_routes = new ArrayList<Route>(POPULATION_SIZE);
		for (int i = 0; i < POPULATION_SIZE; ++i) {
			Collections.shuffle(list);
			City[] route = new City[numCities];
			for (int j = 0; j < numCities; ++j)
				route[j] = list.get(j);
			init_routes.add(new Route(route, calcRouteLength(route)));
		}
		Collections.sort(init_routes);
		//System.out.println(init_routes.size() + " random routes created.");

		// Pick the 100 fittest routes.
		routes = new ArrayList<Route>(EVOLVING_POPULATION_SIZE);
		for (int i = 0; i < EVOLVING_POPULATION_SIZE; ++i)
			routes.add(init_routes.get(i));
		//System.out.println(routes.size() + " fittest routes stored. Top Route: " + routes.get(0).getRouteLength());
		Collections.sort(routes);

	}

	private void createMatrix() {
		matrix = new double[numCities][numCities];
		for (int i = 0; i < numCities - 1; ++i) {
			matrix[i][i] = 0.0;
			for (int j = i + 1; j < numCities; ++j) {
				City city1 = baseCityArray[i];
				City city2 = baseCityArray[j];
				double t1 = (city1.getX() - city2.getX());
				double t2 = (city1.getY() - city2.getY());
				double distance = Math.sqrt(t1 * t1 + t2 * t2);
				matrix[i][j] = distance;
				matrix[j][i] = distance;
			}
		}
		matrix[numCities - 1][numCities - 1] = 0.0;
	}

	private double calcRouteLength(City[] route) {
		double distance = 0.0;
		int c1 = route[0].getId();
		for (int i = 1; i < numCities; ++i) {
			int c2 = route[i].getId();
			distance += matrix[c1][c2];
			c1 = c2;
		}
		distance += matrix[c1][route[0].getId()];
		return distance;
	}

	public void evolve() {

		ArrayList<Route> newChildren = new ArrayList<Route>(90);

		// Create 2 new children, repeat 45 times.
		for (int j = 0; j < 90 / 2; ++j) {

			// Randomly select 6 routes.
			ArrayList<Route> possibleParents = new ArrayList<Route>(6);
			for (int i = 0; i < 6; ++i) {
				int index = generator.nextInt(routes.size() - 1);
				possibleParents.add(routes.get(index));
			}

			// Select the 2 fittest to be parents.
			Collections.sort(possibleParents);
			Route dad = possibleParents.get(0);
			Route mom = possibleParents.get(1);

			// System.out.print("\nDad: ");
			// for (City c : dad.getRoute())
			// System.out.printf("%2d,", c.getId());
			// System.out.print("\nMom: ");
			// for (City c : mom.getRoute())
			// System.out.printf("%2d,", c.getId());

			Route child1, child2;

			// Roll dice for crossover.
			if (generator.nextDouble() > 0.9) {
				child1 = new Route(dad);
				child2 = new Route(mom);
			} else {

				City[] cityList1 = Arrays.copyOf(dad.getRoute(), dad.getRoute().length);
				City[] cityList2 = Arrays.copyOf(mom.getRoute(), mom.getRoute().length);

				int crossoverPoint = generator.nextInt(numCities - 1);
				// System.out.print("\nCrossover Point: " + crossoverPoint);

				Set<City> firstHalf = new HashSet<City>(crossoverPoint + 1);
				for (int i = 0; i < crossoverPoint + 1; ++i)
					firstHalf.add(cityList1[i]);

				int insertPt = crossoverPoint + 1;
				for (int i = 0; i < numCities; ++i) {
					City c = cityList2[i];
					if (!firstHalf.contains(c)) {
						cityList1[insertPt] = c;
						insertPt++;
					}
				}

				firstHalf = new HashSet<City>(crossoverPoint + 1);
				for (int i = 0; i < crossoverPoint + 1; ++i)
					firstHalf.add(cityList2[i]);

				insertPt = crossoverPoint + 1;
				for (int i = 0; i < numCities; ++i) {
					City c = cityList1[i];
					if (!firstHalf.contains(c)) {
						cityList2[insertPt] = c;
						insertPt++;
					}
				}

				// Create children.
				child1 = new Route(cityList1, calcRouteLength(cityList1));
				child2 = new Route(cityList2, calcRouteLength(cityList2));

				// System.out.print("\nC1:  ");
				// for (City c : child1.getRoute())
				// System.out.printf("%2d,", c.getId());
				// System.out.print("\nC2:  ");
				// for (City c : child2.getRoute())
				// System.out.printf("%2d,", c.getId());
			}

			// Roll dice for mutation.
			if (generator.nextDouble() <= 0.1)
				mutate(child1);
			if (generator.nextDouble() <= 0.1)
				mutate(child2);

			newChildren.add(child1);
			newChildren.add(child2);
			// System.out.println("C1:" + child1.getRouteLength() + ", C2:" +
			// child2.getRouteLength());
		}

		ArrayList<Route> temp = new ArrayList<Route>(EVOLVING_POPULATION_SIZE);
		for (int i = 0; i < 10; ++i)
			temp.add(routes.get(i));

		// Insert new generation.
		for (Route r : newChildren)
			temp.add(r);

		routes = temp;
		Collections.sort(routes);

//		System.out.println(routes);
//		for (Route r : routes)
//			System.out.printf("%2f,", calcRouteLength(r.getRoute()));

//		System.out.println("\nGeneration Created. Top Route: " + routes.get(0).getRouteLength());
		double totalLength = 0;
		for (Route r : routes)
			totalLength += r.getRouteLength();
//		System.out.println("Total Length:" + totalLength);

	}

	public void mutate(Route r) {
		// Randomly swap two cities in the route.
		int c1 = generator.nextInt(numCities - 1);
		int c2 = generator.nextInt(numCities - 1);
		City[] cityList = r.getRoute();
		City temp = cityList[c1];
		cityList[c1] = cityList[c2];
		cityList[c2] = temp;
		r.setRoute(cityList);
		r.setRouteLength(calcRouteLength(cityList));
	}

}
