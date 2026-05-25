import cv2 
import numpy as np
from itertools import combinations

def detect_film_frame_hough(img, show_steps=True):
    """
    Wykrywa prostokąt klatki filmowej używając transformaty Hougha
    
    Args:
        img: obraz (może być w skali szarości lub kolorowy)
        show_steps: czy pokazywać etapy przetwarzania
    
    Returns:
        detected_frame: obraz z zaznaczonym prostokątem
        corners: współrzędne narożników prostokąta
    """
    
    original = img.copy()

    base_mask_value = 0    
    # Konwersja do skali szarości (jeśli obraz jest kolorowy)
    if len(img.shape) == 3:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
    else:
        gray = img.copy()
    


    norm = cv2.normalize(gray, None, 0, 255, cv2.NORM_MINMAX)

    left_side_strip = norm[:, :int(gray.shape[1] * 0.1)]  # Lewy pasek (10% szerokości)

    base_mask_value = np.median(left_side_strip)

    treasholded = cv2.threshold(gray, base_mask_value , 255, cv2.THRESH_BINARY)
    cv2.window('Thresholded', cv2.WINDOW_NORMAL)
    cv2.imshow('Thresholded', cv2.resize(treasholded[1], (800, 600)))
    

    # Redukcja szumu
    blurred = cv2.GaussianBlur(treasholded[1], (3, 3), 0)
    sharpend = blurred - cv2.Laplacian(blurred, cv2.CV_64F)
    sharpened = cv2.convertScaleAbs(sharpend)
    
    # Wykrywanie krawędzi - ale tylko do pomocy dla Hougha
    edges = cv2.Canny(blurred, 70, 130, apertureSize=3)
    edges[:, :int(gray.shape[1] * 0.05)] = 0  # Wyłącz krawędzie w lewym pasku
    edges[:, int(gray.shape[1] * 0.95):] = 0  # Wyłącz krawędzie w prawym pasku
    edges[:int(gray.shape[0] * 0.05), :] = 0  # Wyłącz krawędzie w górnym pasku
    edges[int(gray.shape[0] * 0.95):, :] = 0
    

    # Transformata Hougha do wykrywania linii
    # rho: rozdzielczość w pikselach (1 piksel)
    # theta: rozdzielczość kąta (1 stopień = pi/180)
    # threshold: minimalna liczba punktów przecięcia
    lines = cv2.HoughLines(edges, rho=2, theta=np.pi/180, threshold=200)
    
    if lines is None:
        print("✗ Nie wykryto żadnych linii")
        return original, None
    
    # Filtruj linie - szukamy linii poziomych i pionowych
    horizontal_lines = []
    vertical_lines = []
    
    for line in lines:
        rho, theta = line[0]
        
        # Linie poziome: theta ≈ 0° lub ≈ 180°
        if theta < np.pi/4 or theta > 3*np.pi/4:
            horizontal_lines.append((rho, theta))
        # Linie pionowe: theta ≈ 90°
        else:
            vertical_lines.append((rho, theta))
    
    print(f"Znaleziono {len(horizontal_lines)} linii poziomych i {len(vertical_lines)} linii pionowych")
    
    # Filtruj podobne linie (grupowanie)
    horizontal_lines = filter_similar_lines(horizontal_lines, rho_threshold=20)
    vertical_lines = filter_similar_lines(vertical_lines, rho_threshold=20)
    
    print(f"Po filtrowaniu: {len(horizontal_lines)} poziomych i {len(vertical_lines)} pionowych")
    
    if len(horizontal_lines) < 2 or len(vertical_lines) < 2:
        print("✗ Nie znaleziono wystarczającej liczby linii do utworzenia prostokąta")
        return original, None
    
    # Znajdź przecięcia linii (narożniki)
    corners = find_rectangle_from_lines(horizontal_lines, vertical_lines, img.shape)
    
    # Przygotuj obraz do rysowania
    result = original.copy()
    if len(result.shape) == 2:
        result = cv2.cvtColor(result, cv2.COLOR_GRAY2BGR)
    
    # Rysuj wykryte linie (opcjonalnie)
    if show_steps:
        lines_img = result.copy()
        
        # Rysuj linie poziome
        for rho, theta in horizontal_lines:
            draw_line(lines_img, rho, theta, (255, 0, 0), 2)
        
        # Rysuj linie pionowe
        for rho, theta in vertical_lines:
            draw_line(lines_img, rho, theta, (0, 0, 255), 2)
        
        cv2.imshow('Detected Lines', cv2.resize(lines_img, (800, 600)))
    
    if corners is not None:
        # Rysuj prostokąt
        corners_int = corners.astype(np.int32)
        cv2.polylines(result, [corners_int], True, (0, 255, 0), 3)
        
        # Zaznacz narożniki
        for corner in corners_int:
            cv2.circle(result, tuple(corner), 8, (255, 0, 0), -1)
        
        print("✓ Wykryto klatkę filmową!")
        print(f"Narożniki:\n{corners}")
    else:
        print("✗ Nie znaleziono prostokąta")
    
    if show_steps:
        cv2.imshow('Edges', cv2.resize(edges, (800, 600)))
        cv2.imshow('Result', cv2.resize(result, (800, 600)))
    
    return result, corners


def filter_similar_lines(lines, rho_threshold=20, theta_threshold=np.pi/36):
    """
    Filtruje podobne linie (grupowanie) - zostawia tylko unikalne
    """
    if not lines:
        return []
    
    filtered = []
    lines = sorted(lines, key=lambda x: x[0])  # Sortuj po rho
    
    for rho, theta in lines:
        # Sprawdź czy ta linia jest podobna do już dodanych
        is_similar = False
        for f_rho, f_theta in filtered:
            if abs(rho - f_rho) < rho_threshold and abs(theta - f_theta) < theta_threshold:
                is_similar = True
                break
        
        if not is_similar:
            filtered.append((rho, theta))
    
    return filtered


def line_intersection(line1, line2):
    """
    Oblicza punkt przecięcia dwóch linii w postaci (rho, theta)
    """
    rho1, theta1 = line1
    rho2, theta2 = line2
    
    # Macierz współczynników
    A = np.array([
        [np.cos(theta1), np.sin(theta1)],
        [np.cos(theta2), np.sin(theta2)]
    ])
    b = np.array([rho1, rho2])
    
    try:
        x, y = np.linalg.solve(A, b)
        return (x, y)
    except np.linalg.LinAlgError:
        return None


def find_rectangle_from_lines(horizontal_lines, vertical_lines, img_shape):
    """
    Znajduje prostokąt z linii poziomych i pionowych
    """
    height, width = img_shape[:2]
    
    # Sortuj linie według położenia
    horizontal_lines = sorted(horizontal_lines, key=lambda x: x[0])
    vertical_lines = sorted(vertical_lines, key=lambda x: x[0])
    
    # Dla prostokąta potrzebujemy 2 linii poziomych (góra i dół) 
    # i 2 pionowych (lewo i prawo)
    
    # Znajdź najlepszą kombinację linii tworzących prostokąt
    best_rect = None
    best_score = 0
    
    # Spróbuj różnych kombinacji
    for h_pair in combinations(range(len(horizontal_lines)), 2):
        for v_pair in combinations(range(len(vertical_lines)), 2):
            h1, h2 = horizontal_lines[h_pair[0]], horizontal_lines[h_pair[1]]
            v1, v2 = vertical_lines[v_pair[0]], vertical_lines[v_pair[1]]
            
            # Oblicz 4 narożniki
            corners = []
            for h_line in [h1, h2]:
                for v_line in [v1, v2]:
                    intersection = line_intersection(h_line, v_line)
                    if intersection is not None:
                        x, y = intersection
                        # Sprawdź czy punkt jest w granicach obrazu
                        if 0 <= x < width and 0 <= y < height:
                            corners.append([x, y])
            
            if len(corners) == 4:
                corners = np.array(corners, dtype=np.float32)
                
                # Oceń jakość prostokąta
                area = cv2.contourArea(corners)
                perimeter = cv2.arcLength(corners, True)
                
                # Prostokąt powinien mieć dużą powierzchnię
                img_area = width * height
                if area > 0.1 * img_area and area < 0.9 * img_area:
                    # Sprawdź proporcje
                    x, y, w, h = cv2.boundingRect(corners)
                    aspect_ratio = float(w) / h if h > 0 else 0
                    
                    # Oceń na podstawie powierzchni i proporcji
                    score = area
                    
                    # Preferuj proporcje 3:2 (35mm) lub 2:3
                    if 1.3 < aspect_ratio < 1.7 or 0.58 < aspect_ratio < 0.77:
                        score *= 1.5
                    
                    if score > best_score:
                        best_score = score
                        best_rect = corners
    
    # Jeśli nie znaleziono dobrego prostokąta, użyj skrajnych linii
    if best_rect is None and len(horizontal_lines) >= 2 and len(vertical_lines) >= 2:
        # Użyj pierwszej i ostatniej linii z każdej grupy
        h1, h2 = horizontal_lines[0], horizontal_lines[-1]
        v1, v2 = vertical_lines[0], vertical_lines[-1]
        
        corners = []
        for h_line in [h1, h2]:
            for v_line in [v1, v2]:
                intersection = line_intersection(h_line, v_line)
                if intersection is not None:
                    corners.append(list(intersection))
        
        if len(corners) == 4:
            best_rect = np.array(corners, dtype=np.float32)
    
    if best_rect is not None:
        # Sortuj narożniki: top-left, top-right, bottom-right, bottom-left
        best_rect = order_points(best_rect)
    
    return best_rect


def order_points(pts):
    """
    Sortuje punkty w kolejności: top-left, top-right, bottom-right, bottom-left
    """
    # Sumuj współrzędne
    s = pts.sum(axis=1)
    diff = np.diff(pts, axis=1)
    
    rect = np.zeros((4, 2), dtype=np.float32)
    
    # Top-left ma najmniejszą sumę, bottom-right największą
    rect[0] = pts[np.argmin(s)]
    rect[2] = pts[np.argmax(s)]
    
    # Top-right ma najmniejszą różnicę, bottom-left największą
    rect[1] = pts[np.argmin(diff)]
    rect[3] = pts[np.argmax(diff)]
    
    return rect


def draw_line(img, rho, theta, color, thickness=2):
    """
    Rysuje linię w formacie (rho, theta) na obrazie
    """
    a = np.cos(theta)
    b = np.sin(theta)
    x0 = a * rho
    y0 = b * rho
    
    # Punkt na linii
    x1 = int(x0 + 3000 * (-b))
    y1 = int(y0 + 3000 * (a))
    x2 = int(x0 - 3000 * (-b))
    y2 = int(y0 - 3000 * (a))
    
    cv2.line(img, (x1, y1), (x2, y2), color, thickness)


def extract_and_perspective_correct(img, corners):
    """
    Wyodrębnia i koryguje perspektywę klatki filmowej
    
    Args:
        img: obraz
        corners: współrzędne 4 narożników (już posortowane)
    
    Returns:
        corrected: skorygowany obraz klatki
    """
    if corners is None:
        return None
    
    # Wymiary docelowe (standardowa proporcja 35mm: 36x24mm = 3:2)
    width = 1200
    height = 800
    
    dst_points = np.array([
        [0, 0],
        [width - 1, 0],
        [width - 1, height - 1],
        [0, height - 1]
    ], dtype=np.float32)
    
    # Oblicz macierz transformacji perspektywy
    M = cv2.getPerspectiveTransform(corners, dst_points)
    
    # Zastosuj transformację
    corrected = cv2.warpPerspective(img, M, (width, height))
    
    return corrected
