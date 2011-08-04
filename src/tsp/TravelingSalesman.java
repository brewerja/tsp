package tsp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class TravelingSalesman {

	public static void main(String[] args) {
		TravelingSalesman ts = new TravelingSalesman(args[0]);
	}

	private int numCities;
	private City[] baseCityArray;
	private ArrayList<Route> routes;
	private double[][] matrix;
	private final int POPULATION_SIZE = 1000;

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
		System.out.println(numCities + " cities read.");

		// Create the base route.
		baseCityArray = new City[numCities];
		for (int i = 0; i < numCities; ++i)
			baseCityArray[i] = list.get(i);

		// Create matrix to store distances between cities.
		createMatrix();

		// Create the initial population of randomized routes.
		routes = new ArrayList<Route>(POPULATION_SIZE);
		for (int i = 0; i < POPULATION_SIZE; ++i) {
			Collections.shuffle(list);
			City[] route = new City[numCities];
			for (int j = 0; j < numCities; ++j)
				route[j] = list.get(j);
			routes.add(new Route(route, calcRouteLength(route)));
		}
		Collections.sort(routes);
		System.out.println(routes.size() + " random routes created.");
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

}
