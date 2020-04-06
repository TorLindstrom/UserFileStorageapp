package tor;

/*Code comments – why and NOT how.
• User files shall be stored in a folder with their name.
• User is only allowed to interact with folders & files inside their own user folder.
• User shall be able to Read/Move/Copy/Delete/Create folders & files.
• User shall be able to clear content of a text file(.txt) or add new text at the end.*/

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class UserFileStorageApp {
    private Path currentFolder;
    private Path userFolder;
    private User currentUser;
    private HashSet<User> users;
    private boolean consoleAvailable = true;
    private Scanner scanner = new Scanner(System.in);
    private Console console;

    {
        console = System.console();
        if (console == null) {
            consoleAvailable = false;
        }
    }

    //Static inner class as instances are not dependent on the userFileStorage instance,
    //but inner class as users are only relevant within the program
    static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        private static AtomicInteger counter = new AtomicInteger(0); //used to give unique id's, saved when closing
        private int id; //used for access, not changeable
        private String name; //used for login and user interaction can't be duplicates
        private char[] password;

        private User(String name, char[] password) {
            this.id = counter.getAndIncrement();
            this.name = name;
            this.password = password;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof User && name.equals(((User) obj).name);
        }

        @Override
        public int hashCode() {
            return name.charAt(0);
        }
    }

    //as the program starts, it needs relevant data
    private UserFileStorageApp() {
        readUsers();
        if (users == null) {
            users = new HashSet<>();
        }
        loadCounter();
    }

    public static void main(String[] args) {
        UserFileStorageApp app = new UserFileStorageApp();
        app.login();
        app.menu();
    }

    private void login() {
        String userName;
        char[] passWord;
        name:
        while (true) {
            System.out.print("Please enter your user name:    ");
            userName = readInput().trim();
            for (User user : users) {
                if (user.name.equals(userName)) {
                    currentUser = user;
                    break name;
                }
            }
            System.out.println("User name not found, create new user?   (y) / (n)");
            switch (readInput().toLowerCase()) {
                case "y":
                    break name;
                case "n":
                default:
                    System.out.println("Returning");
                    continue name;
            }
        }
        for (int i = 0; i < 3; i++) {
            System.out.print("Please enter your pass word:    ");
            passWord = readPassword();
            if (currentUser == null) {
                User user = new User(userName, passWord);
                users.add(user);
                currentUser = user;
                navigateStartFolder();
                return;
            }
            if (Arrays.equals(currentUser.password, passWord)) {
                System.out.println("Welcome back, " + currentUser.name);
                navigateStartFolder();
                return;
            }
        }
        currentUser = null;
        System.out.println("Too many incorrect attempts, aborting");
    }

    private void menu() {
        loop: //loops until exit
        while (true) {
            listEntries();
            System.out.println("Please choose action:\n\t(v)iew\n\t(..) to go back\n\t(s)tart folder\n\t(n)ew\n\t(d)elete\n\t(m)ove\n\t(c)opy\n\tchange (u)ser name\n\t(l)ogout\n\tor\n\t(e)xit");
            String[] input = readInput().split(" ");
            switch (input[0].toLowerCase()) {
                case "v":
                    if (input.length > 1) view(currentFolder.resolve(Paths.get(input[1])));
                    else view();
                    break;
                case "..":
                    view(currentFolder.getParent());
                    break;
                case "s":
                    navigateStartFolder();
                    view(currentFolder);
                    break;
                case "n":
                    if (input.length > 1) create(Paths.get(input[1]));
                    else create();
                    break;
                case "d":
                    if (input.length > 1) delete(Paths.get(input[1]));
                    else delete();
                    break;
                case "m":
                    if (input.length > 2) move(Paths.get(input[1]), Paths.get(input[2]));
                    else move();
                    break;
                case "c":
                    if (input.length > 2) copy(Paths.get(input[1]), Paths.get(input[2]));
                    else copy();
                    break;
                case "u":
                    nameChange();
                    break;
                case "l":
                    logoff();
                    login();
                    break;
                case "e":
                    logoff();
                    break loop;
                default:
                    System.out.println("Invalid command");
            }
        }
    }


    //--------- Menu items
    private void view(Path path) {
        if (!checkValidPath(path) || !path.toFile().exists()) {
            System.out.println("Can't view");
            return;
        }
        if (Files.isDirectory(path)) {
            String toDisplay = path.getFileName().toString();
            if (toDisplay.equals(String.valueOf(currentUser.id))) {
                toDisplay = "start";
            }
            System.out.println("--Current folder:" + toDisplay + "--");
            currentFolder = path;
        } else {
            readFile(path);
        }
    }

    private void view() {
        System.out.println("Either write a complete path from here, or write the name of an entry");
        Path path = currentFolder.resolve(Paths.get(readInput()));
        view(path);
    }

    private void create(Path path) {
        try {
            Path completePath = currentFolder.resolve(path);
            System.out.println(completePath);
            if (!checkValidPath(completePath)) {
                System.out.println("Invalid");
                return;
            }
            if (completePath.toFile().exists()) {
                System.out.println("exists");
                return;
            }
            if (!completePath.toString().contains(".")) {
                System.out.println("Directory");
                Files.createDirectories(completePath);
                currentFolder = completePath;
                view(currentFolder);
            } else {
                System.out.println("File");
                Path parent = completePath.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }
                Files.createFile(completePath);
                view(parent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void create() {
        System.out.println("Either write a complete path from here, or create an entry");
        Path path = Paths.get(readInput());
        create(path);
    }

    private void delete(Path path) {
        try {
            Path completePath = currentFolder.resolve(path);
            System.out.println(completePath);
            if (!checkValidPath(completePath)) {
                System.out.println("Invalid");
                return;
            }
            if (!completePath.toFile().exists()) {
                System.out.println("Does not exist");
                return;
            }
            if (Files.isDirectory(completePath)) {
                recursiveDelete(completePath);
                currentFolder = completePath.getParent();
                view(currentFolder);
            } else {
                Files.delete(completePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void delete() {
        System.out.println("Write the name of the file or directory to delete");
        Path path = Paths.get(readInput());
        delete(path);
    }

    //called for every directory to be looked through and deleted
    private void recursiveDelete(Path path) {
        try {
            String[] entries = path.toFile().list();
            if (entries != null) {
                for (String entry : entries) {
                    Path currentEntry = path.resolve(Paths.get(entry));
                    System.out.println(currentEntry);
                    if (Files.isDirectory(currentEntry)) {
                        recursiveDelete(currentEntry);
                    } else {
                        Files.delete(currentEntry);
                        System.out.println("DELETED FILE: " + currentEntry.getFileName());
                    }
                }
            }
            Files.delete(path);
            System.out.println("DELETED DIRECTORY: " + path.getFileName());
        } catch (IOException e) {
            System.out.println("CLOSURE DELETE PROBLEM");
            e.printStackTrace();
        }
    }

    private void move(Path toBeMoved, Path target) {
        if (target.toString().contains(".")) {
            System.out.println("Target is not a folder");
            return;
        }
        toBeMoved = currentFolder.resolve(toBeMoved);
        target = currentFolder.resolve(target);
        try {
            if (Files.exists(toBeMoved)) {
                if (Files.isDirectory(toBeMoved)) {
                    recursiveMove(toBeMoved, target);
                } else {
                    Files.move(toBeMoved, target.resolve(toBeMoved.getFileName()));
                }
            } else {
                System.out.println("FILE NOT FOUND");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentFolder = target;
        view(target);
    }

    private void move() {
        System.out.println("Write the name of the file or directory to be moved");
        Path path1 = Paths.get(readInput());
        System.out.println("Write the name of the directory to move to");
        Path path2 = Paths.get(readInput());
        move(path1, path2);
    }

    //called for every directory to be looked through and moved
    private void recursiveMove(Path toBeMoved, Path target) {
        try {
            Files.copy(toBeMoved, target.resolve(toBeMoved.getFileName()));
            Files.list(toBeMoved).forEach((Path a) ->
            {
                try {
                    Path subTarget = target.resolve(toBeMoved.getFileName());
                    if (Files.isDirectory(a)) {
                        recursiveMove(a, subTarget);
                    } else {
                        Files.move(a, subTarget.resolve(a.getFileName()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            Files.delete(toBeMoved);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void copy(Path toBeCopied, Path target) {
        if (target.toString().contains(".")) {
            System.out.println("Target is not a folder");
            return;
        }
        toBeCopied = currentFolder.resolve(toBeCopied);
        target = currentFolder.resolve(target);
        try {
            if (Files.isDirectory(toBeCopied)) {
                recursiveCopy(toBeCopied, target);
            } else {
                Files.copy(toBeCopied, target.resolve(toBeCopied.getFileName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentFolder = target;
        view(target);
    }

    private void copy() {
        System.out.println("Write the name of the file or directory to copy");
        Path path1 = Paths.get(readInput());
        System.out.println("Write the name of the directory to copy to");
        Path path2 = Paths.get(readInput());
        copy(path1, path2);
    }

    //called for every directory to be looked through and copied
    private void recursiveCopy(Path toBeCopied, Path target) {
        try {
            Files.copy(toBeCopied, target.resolve(toBeCopied.getFileName()));
            Files.list(toBeCopied).forEach((Path a) ->
            {
                try {
                    Path subTarget = target.resolve(toBeCopied.getFileName());
                    if (Files.isDirectory(a)) {
                        recursiveCopy(a, subTarget);
                    } else {
                        Files.copy(a, subTarget.resolve(a.getFileName()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void nameChange() {
        System.out.print("Please enter your user name:    ");
        String userName = readInput();
        for (User user : users) {
            if (user.name.equals(userName)) {
                System.out.println("Name unavailable");
                return;
            }
        }
        currentUser.name = userName;
    }

    private void readFile(Path path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //---------


    //--------- Helpers
    private void navigateStartFolder() {
        String name = String.valueOf(currentUser.id);
        Path path = Paths.get("FileUsers", name);
        if (!Files.exists(path)) {
            createDir(path);
        }
        currentFolder = path;
        userFolder = path;
    }

    private void listEntries() {
        for (String string : currentFolder.toFile().list()) {
            if (Files.isDirectory(Paths.get(string))) {
                string = "* " + string;
            } else {
                string = "    " + string;
            }
            System.out.println(string);
        }
        System.out.println();
    }

    private void createDir(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readInput() {
        if (consoleAvailable) {
            return console.readLine();
        } else {
            return scanner.nextLine();
        }
    }

    private char[] readPassword() {
        if (consoleAvailable) {
            return console.readPassword();
        } else {
            return scanner.nextLine().toCharArray();
        }
    }

    //check to see that the path doesn't leave the user folder
    private boolean checkValidPath(Path path) {
        path = path.toAbsolutePath().normalize();
        System.out.println("To be validated " + path);
        return path.startsWith(userFolder.toAbsolutePath());
    }
    //---------


    //--------- Save and load users and counter
    private void logoff() {
        currentUser = null;
        currentFolder = null;
        saveCounter();
        saveUsers();
    }

    private void saveUsers() {
        try (ObjectOutputStream saver = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("savedUsers.ser")))) {
            saver.writeObject(users);
        } catch (FileNotFoundException e) {
            remakeFile("savedUsers.ser");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveCounter() {
        try (FileOutputStream saver = new FileOutputStream("counter.data")) {
            saver.write(User.counter.get());
        } catch (FileNotFoundException e) {
            remakeFile("counter.data");
            saveCounter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void remakeFile(String pathName) {
        try {
            new File(pathName).createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean readUsers() {
        try (ObjectInputStream loader = new ObjectInputStream(new BufferedInputStream(new FileInputStream("savedUsers.ser")))) {
            users = (HashSet<User>) loader.readObject();
        } catch (FileNotFoundException e) {
            remakeFile("savedUsers.ser");
        } catch (EOFException e) {
            System.out.println("Successfully read");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void loadCounter() {
        try (FileInputStream reader = new FileInputStream("counter.data")) {
            User.counter = new AtomicInteger(reader.read());
        } catch (FileNotFoundException e) {
            System.out.println("Counter not saved, catching up.");
            catchUpCounter();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void catchUpCounter() {
        int highest = 0;
        for (User user : users) {
            if (user.id > highest) {
                highest = user.id;
            }
        }
        User.counter = new AtomicInteger(highest + 1);
    }
    //---------
}
